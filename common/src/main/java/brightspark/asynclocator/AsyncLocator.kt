package brightspark.asynclocator

import brightspark.asynclocator.platform.Services
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
import java.time.temporal.ChronoUnit
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
  private var LOCATING_EXECUTOR_SERVICE: ExecutorService? = null

  fun setupExecutorService() {
    shutdownExecutorService()

    val threads = Services.CONFIG.locatorThreads()
    LOG.info("Starting locating executor service with thread pool size of {}", threads)
    LOCATING_EXECUTOR_SERVICE = Executors.newFixedThreadPool(
      threads,
      object : ThreadFactory {
        private val poolNum = AtomicInteger(1)
        private val threadNum = AtomicInteger(1)
        private val namePrefix: String = MOD_ID + "-" + poolNum.getAndIncrement() + "-thread-"

        override fun newThread(r: Runnable): Thread {
          return Thread(r, namePrefix + threadNum.getAndIncrement())
        }
      }
    )
  }

  fun shutdownExecutorService() {
    if (LOCATING_EXECUTOR_SERVICE != null) {
      ALConstants.logInfo("Shutting down locating executor service")
      LOCATING_EXECUTOR_SERVICE!!.shutdown()
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
    skipKnownStructures: Boolean
  ): LocateTask<BlockPos?> {
    ALConstants.logDebug(
      "Creating locate task for {} in {} around {} within {} chunks",
      structureTag, level, pos, searchRadius
    )
    val completableFuture = CompletableFuture<BlockPos?>()
    val future = LOCATING_EXECUTOR_SERVICE!!.submit {
      try {
        doLocateStructureLevel(
          completableFuture,
          level,
          structureTag,
          pos,
          searchRadius,
          skipKnownStructures
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
    skipKnownStructures: Boolean
  ): LocateTask<Pair<BlockPos, Holder<Structure>>?> {
    ALConstants.logDebug(
      "Creating locate task for {} in {} around {} within {} chunks",
      structureSet, level, pos, searchRadius
    )
    val completableFuture = CompletableFuture<Pair<BlockPos, Holder<Structure>>?>()
    val future = LOCATING_EXECUTOR_SERVICE!!.submit {
      doLocateStructureChunkGenerator(
        completableFuture,
        level,
        structureSet,
        pos,
        searchRadius,
        skipKnownStructures
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
    skipExistingChunks: Boolean
  ) {
    ALConstants.logInfo(
      "Trying to locate {} in {} around {} within {} chunks",
      structureTag, level, pos, searchRadius
    )

    val foundPos = executeSearchWithLogging(
      structureTag
    ) { level.findNearestMapStructure(structureTag, pos, searchRadius, skipExistingChunks) }
    completableFuture.complete(foundPos)
  }

  private fun doLocateStructureChunkGenerator(
    completableFuture: CompletableFuture<Pair<BlockPos, Holder<Structure>>?>,
    level: ServerLevel,
    structureSet: HolderSet<Structure>,
    pos: BlockPos,
    searchRadius: Int,
    skipExistingChunks: Boolean
  ) {
    ALConstants.logInfo(
      "Trying to locate {} in {} around {} within {} chunks",
      structureSet, level, pos, searchRadius
    )

    val foundPos = executeSearchWithLogging(
      structureSet,
      Supplier<Pair<BlockPos, Holder<Structure>>?> {
        level.chunkSource.generator
          .findNearestMapStructure(level, structureSet, pos, searchRadius, skipExistingChunks)
      },
      Function<Pair<BlockPos?, Holder<Structure?>?>?, BlockPos?> { pair: Pair<BlockPos?, Holder<Structure?>?>? -> pair?.first }
    )

    completableFuture.complete(foundPos)
  }

  /**
   * Queues a task to locate a feature using [ServerLevel.findClosestBiome3d]
   * and returns a [LocateTask] with the futures for it.
   */
  fun locateBiome(
    level: ServerLevel,
    biomeSet: ResourceOrTagArgument.Result<*>,
    pos: BlockPos,
    searchRadius: Int,
    skipKnownBiomes: Boolean
  ): LocateTask<Pair<BlockPos, Holder<Biome>>?> {
    ALConstants.logDebug(
      "Creating locate task for {} in {} around {} within {} chunks",
      biomeSet, level, pos, searchRadius
    )
    val completableFuture = CompletableFuture<Pair<BlockPos, Holder<Biome>>?>()
    val future = LOCATING_EXECUTOR_SERVICE!!.submit {
      doLocateBiome(
        completableFuture,
        level,
        biomeSet,
        pos,
        searchRadius,
        skipKnownBiomes
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
    skipExistingChunks: Boolean
  ) {
    ALConstants.logInfo(
      "Trying to locate {} in {} around {} within {} chunks",
      biomeSet, level, pos, searchRadius
    )

    val locate = executeSearchWithLogging(
      biomeSet,
      Supplier<Pair<BlockPos, Holder<Biome>>?> {
        val radius = searchRadius * 16
        val horizontalStep = 32
        val verticalStep = 64
        level.findClosestBiome3d(
          biomeSet,
          pos,
          radius,
          horizontalStep,
          verticalStep
        )
      },
      Function<Pair<BlockPos?, Holder<Biome?>?>?, BlockPos?> { result: Pair<BlockPos?, Holder<Biome?>?>? -> result?.first }
    )
    completableFuture.complete(locate)
  }

  private fun executeSearchWithLogging(searchTarget: Any, searchOperation: Supplier<BlockPos?>): BlockPos? {
    return executeSearchWithLogging(
      searchTarget, searchOperation
    ) { blockPos: BlockPos? -> blockPos }
  }

  private fun <T> executeSearchWithLogging(
    searchTarget: Any,
    searchOperation: Supplier<T>,
    positionExtractor: Function<T, BlockPos?>
  ): T? {
    ALConstants.logInfo("Trying to locate {}", searchTarget)
    val start = System.nanoTime()
    val result: T? = searchOperation.get()
    val duration = Duration.ofNanos(System.nanoTime() - start)
    val durationMillis = duration[ChronoUnit.MILLIS]

    if (result != null) {
      val pos = positionExtractor.apply(result)
      ALConstants.logInfo("Found {} at {} (took {}ms or {})", searchTarget, pos, durationMillis, duration)
    } else {
      ALConstants.logInfo("No {} found (took {}ms or {})", searchTarget, durationMillis, duration)
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
    val taskFuture: Future<*>
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
