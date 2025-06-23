package brightspark.asynclocator

import brightspark.asynclocator.AsyncLocatorMod.CONFIG
import brightspark.asynclocator.AsyncLocatorMod.MOD_ID
import brightspark.asynclocator.extensions.LOG
import com.mojang.datafixers.util.Pair
import net.minecraft.commands.arguments.ResourceOrTagArgument
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.HolderSet
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.TagKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.levelgen.structure.Structure
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier

object AsyncLocator {
  private var locatingExecutorService: ExecutorService? = null

  fun setupExecutorService() {
    shutdownExecutorService()

    val threads = CONFIG.locatorThreads.get()
    LOG.info("Starting locating executor service with thread pool size of {}", threads)
    locatingExecutorService =
      Executors.newFixedThreadPool(
        threads,
        object : ThreadFactory {
          private val poolNum = AtomicInteger(1)
          private val threadNum = AtomicInteger(1)
          private val namePrefix: String = MOD_ID + "-" + poolNum.getAndIncrement() + "-thread-"

          override fun newThread(r: Runnable): Thread = Thread(r, namePrefix + threadNum.getAndIncrement())
        },
      )
  }

  fun shutdownExecutorService() {
    if (locatingExecutorService != null) {
      LOG.info("Shutting down locating executor service")
      locatingExecutorService!!.shutdown()
    }
  }

  /**
   * Queues a task to locate a feature using [ServerLevel.findNearestMapStructure]
   * and returns a [LocateTask] with the futures for it.
   */
  fun locateStructure(
    level: ServerLevel,
    structureTag: TagKey<Structure>,
    pos: BlockPos,
    searchRadius: Int,
    skipKnownStructures: Boolean,
  ): LocateTask<BlockPos?> {
    LOG.debug(
      "Creating locate task for {} in {} around {} within {} chunks",
      structureTag,
      level,
      pos,
      searchRadius,
    )
    val completableFuture = CompletableFuture<BlockPos?>()
    val future =
      locatingExecutorService!!.submit {
        try {
          doLocateStructureLevel(
            completableFuture,
            level,
            structureTag,
            pos,
            searchRadius,
            skipKnownStructures,
          )
        } catch (e: InterruptedException) {
          throw RuntimeException(e)
        }
      }
    return LocateTask(level.server, completableFuture, future)
  }

  /**
   * Queues a task to locate a feature using
   * [ChunkGenerator.findNearestMapStructure] and returns a
   * [LocateTask] with the futures for it.
   */
  fun locateStructure(
    level: ServerLevel,
    structureSet: HolderSet<Structure>,
    pos: BlockPos,
    searchRadius: Int,
    skipKnownStructures: Boolean,
  ): LocateTask<Pair<BlockPos, Holder<Structure>>?> {
    LOG.info(
      "Creating locate task for {} in {} around {} within {} chunks",
      structureSet,
      level,
      pos,
      searchRadius,
    )
    val completableFuture = CompletableFuture<Pair<BlockPos, Holder<Structure>>?>()
    val future =
      locatingExecutorService!!.submit {
        doLocateStructureChunkGenerator(
          completableFuture,
          level,
          structureSet,
          pos,
          searchRadius,
          skipKnownStructures,
        )
      }
    return LocateTask(level.server, completableFuture, future)
  }

  @Throws(InterruptedException::class)
  private fun doLocateStructureLevel(
    completableFuture: CompletableFuture<BlockPos?>,
    level: ServerLevel,
    structureTag: TagKey<Structure>,
    pos: BlockPos,
    searchRadius: Int,
    skipExistingChunks: Boolean,
  ) {
    LOG.info(
      "Trying to locate {} in {} around {} within {} chunks",
      structureTag,
      level,
      pos,
      searchRadius,
    )

    val foundPos =
      executeSearchWithLogging(
        structureTag,
      ) { level.findNearestMapStructure(structureTag, pos, searchRadius, skipExistingChunks) }
    completableFuture.complete(foundPos)
  }

  private fun doLocateStructureChunkGenerator(
    completableFuture: CompletableFuture<Pair<BlockPos, Holder<Structure>>?>,
    level: ServerLevel,
    structureSet: HolderSet<Structure>,
    pos: BlockPos,
    searchRadius: Int,
    skipExistingChunks: Boolean,
  ) {
    LOG.info(
      "Trying to locate {} in {} around {} within {} chunks",
      structureSet,
      level,
      pos,
      searchRadius,
    )

    val foundPos =
      executeSearchWithLogging(
        structureSet,
        {
          level.chunkSource.generator
            .findNearestMapStructure(level, structureSet, pos, searchRadius, skipExistingChunks)
        },
        { it?.first },
      )

    completableFuture.complete(foundPos)
  }

  /**
   * Queues a task to locate a feature using [ServerLevel.findClosestBiome3d]
   * and returns a [LocateTask] with the futures for it.
   */
  @JvmStatic
  fun locateBiome(
    level: ServerLevel,
    biomeSet: ResourceOrTagArgument.Result<Biome>,
    pos: BlockPos,
    searchRadius: Int,
    skipKnownBiomes: Boolean,
  ): LocateTask<Pair<BlockPos, Holder<Biome>>?> {
    LOG.info(
      "Creating locate task for {} in {} around {} within {} chunks",
      biomeSet,
      level,
      pos,
      searchRadius,
    )
    val completableFuture = CompletableFuture<Pair<BlockPos, Holder<Biome>>?>()
    val future =
      locatingExecutorService!!.submit {
        doLocateBiome(
          completableFuture,
          level,
          biomeSet,
          pos,
          searchRadius,
          skipKnownBiomes,
        )
      }
    return LocateTask(level.server, completableFuture, future)
  }

  private fun doLocateBiome(
    completableFuture: CompletableFuture<Pair<BlockPos, Holder<Biome>>?>,
    level: ServerLevel,
    biomeSet: ResourceOrTagArgument.Result<Biome>,
    pos: BlockPos,
    searchRadius: Int,
    skipExistingChunks: Boolean,
  ) {
    LOG.info(
      "Trying to locate {} in {} around {} within {} chunks",
      biomeSet,
      level,
      pos,
      searchRadius,
    )

    val locate =
      executeSearchWithLogging(
        biomeSet,
        {
          val radius = searchRadius * 16
          val horizontalStep = 32
          val verticalStep = 64
          level.findClosestBiome3d(
            biomeSet,
            pos,
            radius,
            horizontalStep,
            verticalStep,
          )
        },
        { it?.first },
      )
    completableFuture.complete(locate)
  }

  private fun executeSearchWithLogging(
    searchTarget: Any,
    searchOperation: Supplier<BlockPos?>,
  ): BlockPos? = executeSearchWithLogging(searchTarget, searchOperation) { blockPos: BlockPos? -> blockPos }

  private fun <T> executeSearchWithLogging(
    searchTarget: Any,
    searchOperation: Supplier<T>,
    positionExtractor: Function<T, BlockPos?>,
  ): T? {
    LOG.info("Trying to locate {}", searchTarget)
    val start = System.nanoTime()
    val result: T? = searchOperation.get()
    val duration = Duration.ofNanos(System.nanoTime() - start)
    val durationMillis = duration.toMillis()

    if (result != null) {
      val pos = positionExtractor.apply(result)
      LOG.info("Found {} at {} (took {}ms or {})", searchTarget, pos, durationMillis, duration)
    } else {
      LOG.info("No {} found (took {}ms or {})", searchTarget, durationMillis, duration)
    }

    return result
  }

  /**
   * Holder of the futures for an async locate task as well as providing some helper functions.
   * The completableFuture will be completed once the call to
   * [ServerLevel.findNearestMapStructure] has completed, and will hold the
   * result of it.
   * The taskFuture is the future for the [Runnable] itself in the executor service.
   */
  @JvmRecord
  data class LocateTask<T>(
    val server: MinecraftServer,
    val completableFuture: CompletableFuture<T>,
    val taskFuture: Future<*>,
  ) {
    /**
     * Helper function that calls [CompletableFuture.thenAccept] with the given action.
     * Bear in mind that the action will be executed from the task's thread. If you intend to change any game data,
     * it's strongly advised you use [.thenOnServerThread] instead so that it's queued and executed
     * on the main server thread instead.
     */
    fun then(action: Consumer<T>): LocateTask<T> {
      completableFuture.thenAccept(action)
      return this
    }

    /**
     * Helper function that calls [CompletableFuture.thenAccept] with the given action on the server
     * thread.
     */
    fun thenOnServerThread(action: Consumer<T>): LocateTask<T> {
      completableFuture.thenAccept(Consumer { pos: T -> server.submit { action.accept(pos) } })
      return this
    }

    /**
     * Helper function that cancels both completableFuture and taskFuture.
     */
    fun cancel() {
      taskFuture.cancel(true)
      completableFuture.cancel(false)
    }
  }
}
