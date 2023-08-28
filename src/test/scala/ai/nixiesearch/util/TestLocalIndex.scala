package ai.nixiesearch.util

import ai.nixiesearch.config.StoreConfig
import ai.nixiesearch.config.StoreConfig.LocalStoreConfig
import ai.nixiesearch.config.StoreConfig.StoreUrl.LocalStoreUrl
import ai.nixiesearch.config.mapping.IndexMapping
import ai.nixiesearch.index.LocalIndex
import ai.nixiesearch.index.store.rw.{StoreReader, StoreWriter}
import ai.nixiesearch.index.store.{LocalStore, Store}
import cats.effect.IO
import cats.effect.unsafe.implicits.global

import java.nio.file.{Files, Path}

object TestLocalIndex {
  def apply(index: IndexMapping = TestIndexMapping()): LocalIndex = {
    val dir = Files.createTempDirectory("nixie")
    dir.toFile.deleteOnExit()
    LocalIndex(LocalStoreConfig(LocalStoreUrl(dir.toString)), index)
  }
}
