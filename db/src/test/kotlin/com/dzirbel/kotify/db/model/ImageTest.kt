package com.dzirbel.kotify.db.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.dzirbel.kotify.db.DatabaseExtension
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.StringIdTable
import com.dzirbel.kotify.db.blockingTransaction
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(DatabaseExtension::class)
class ImageTest {
    private object TestEntityTable : StringIdTable()

    private object TestImageEntityTable : Table() {
        val image = reference("image", ImageTable)
        val entity = reference("entity", TestEntityTable)
    }

    @BeforeEach
    fun setup() {
        KotifyDatabase.blockingTransaction { SchemaUtils.create(TestImageEntityTable, TestEntityTable) }
    }

    @AfterEach
    fun cleanup() {
        KotifyDatabase.blockingTransaction { SchemaUtils.drop(TestImageEntityTable, TestEntityTable) }
    }

    @Test
    fun `smallestLargerThan simple`() {
        val image = KotifyDatabase.blockingTransaction {
            val entityId = TestEntityTable.insertAndGetId { it[id] = "id" }

            insertImage(url = "url1", width = 100, height = 100, entityId = entityId)
            insertImage(url = "url2", width = 200, height = 200, entityId = entityId)
            insertImage(url = "url3", width = 300, height = 300, entityId = entityId)
            insertImage(url = "url4", width = null, height = null, entityId = entityId)

            ImageTable.smallestLargerThan(
                joinColumn = TestImageEntityTable.entity,
                id = entityId.value,
                size = ImageSize(width = 150, height = 150),
            )
        }

        assertThat(image).isEqualTo("url2")
    }

    @Test
    fun `smallestLargerThan complex`() {
        val image = KotifyDatabase.blockingTransaction {
            val entityId = TestEntityTable.insertAndGetId { it[id] = "id" }

            insertImage(url = "url1", width = 100, height = 100, entityId = entityId)
            insertImage(url = "url2", width = 140, height = 160, entityId = entityId)
            insertImage(url = "url3", width = 200, height = 200, entityId = entityId)
            insertImage(url = "url4", width = 180, height = 250, entityId = entityId)
            insertImage(url = "url5", width = 300, height = 300, entityId = entityId)
            insertImage(url = "url6", width = null, height = null, entityId = entityId)

            ImageTable.smallestLargerThan(
                joinColumn = TestImageEntityTable.entity,
                id = entityId.value,
                size = ImageSize(width = 150, height = 150),
            )
        }

        assertThat(image).isEqualTo("url3")
    }

    @Test
    fun `smallestLargerThan exact match`() {
        val image = KotifyDatabase.blockingTransaction {
            val entityId = TestEntityTable.insertAndGetId { it[id] = "id" }

            insertImage(url = "url1", width = 100, height = 100, entityId = entityId)
            insertImage(url = "url2", width = 200, height = 200, entityId = entityId)
            insertImage(url = "url3", width = 300, height = 300, entityId = entityId)
            insertImage(url = "url4", width = null, height = null, entityId = entityId)

            ImageTable.smallestLargerThan(
                joinColumn = TestImageEntityTable.entity,
                id = entityId.value,
                size = ImageSize(width = 200, height = 200),
            )
        }

        assertThat(image).isEqualTo("url2")
    }

    @Test
    fun `smallestLargerThan minimum`() {
        val image = KotifyDatabase.blockingTransaction {
            val entityId = TestEntityTable.insertAndGetId { it[id] = "id" }

            insertImage(url = "url1", width = 100, height = 100, entityId = entityId)
            insertImage(url = "url2", width = 200, height = 200, entityId = entityId)
            insertImage(url = "url3", width = 300, height = 300, entityId = entityId)
            insertImage(url = "url4", width = null, height = null, entityId = entityId)

            ImageTable.smallestLargerThan(
                joinColumn = TestImageEntityTable.entity,
                id = entityId.value,
                size = ImageSize(width = 50, height = 50),
            )
        }

        assertThat(image).isEqualTo("url1")
    }

    @Test
    fun `smallestLargerThan larger than largest image`() {
        val image = KotifyDatabase.blockingTransaction {
            val entityId = TestEntityTable.insertAndGetId { it[id] = "id" }

            insertImage(url = "url1", width = 100, height = 100, entityId = entityId)
            insertImage(url = "url2", width = 200, height = 200, entityId = entityId)
            insertImage(url = "url3", width = 300, height = 300, entityId = entityId)
            insertImage(url = "url4", width = 200, height = 400, entityId = entityId)
            insertImage(url = "url5", width = null, height = null, entityId = entityId)

            ImageTable.smallestLargerThan(
                joinColumn = TestImageEntityTable.entity,
                id = entityId.value,
                size = ImageSize(width = 500, height = 500),
            )
        }

        assertThat(image).isEqualTo("url3")
    }

    @Test
    fun `findOrCreate new`() {
        val image = KotifyDatabase.blockingTransaction {
            Image.findOrCreate(url = "url", width = 100, height = 100)
        }

        assertThat(image.url).isEqualTo("url")
        assertThat(image.width).isEqualTo(100)
        assertThat(image.height).isEqualTo(100)
    }

    @Test
    fun `findOrCreate existing new size`() {
        val image1 = KotifyDatabase.blockingTransaction {
            Image.findOrCreate(url = "url", width = 100, height = 100)
        }

        val image2 = KotifyDatabase.blockingTransaction {
            Image.findOrCreate(url = "url", width = 200, height = 200)
        }

        assertThat(image2.id).isEqualTo(image1.id)
        assertThat(image2.url).isEqualTo(image1.url)
        assertThat(image2.width).isEqualTo(image2.width)
        assertThat(image2.height).isEqualTo(image2.height)
    }

    @Test
    fun `findOrCreate existing null size`() {
        val image1 = KotifyDatabase.blockingTransaction {
            Image.findOrCreate(url = "url", width = 100, height = 100)
        }

        val image2 = KotifyDatabase.blockingTransaction {
            Image.findOrCreate(url = "url", width = null, height = null)
        }

        assertThat(image2.id).isEqualTo(image1.id)
        assertThat(image2.url).isEqualTo(image1.url)
        assertThat(image2.width).isEqualTo(image1.width)
        assertThat(image2.height).isEqualTo(image1.height)
    }

    @Test
    fun `findOrCreate existing same size`() {
        val image1 = KotifyDatabase.blockingTransaction {
            Image.findOrCreate(url = "url", width = 100, height = 100)
        }

        val image2 = KotifyDatabase.blockingTransaction {
            Image.findOrCreate(url = "url", width = 100, height = 100)
        }

        assertThat(image2.id).isEqualTo(image1.id)
        assertThat(image2.url).isEqualTo(image1.url)
        assertThat(image2.width).isEqualTo(100)
        assertThat(image2.height).isEqualTo(100)
    }

    private fun insertImage(url: String, width: Int?, height: Int?, entityId: EntityID<String>) {
        val imageId = ImageTable.insertAndGetId { statement ->
            statement[ImageTable.url] = url
            statement[ImageTable.width] = width
            statement[ImageTable.height] = height
        }
        TestImageEntityTable.insert { statement ->
            statement[image] = imageId
            statement[entity] = entityId
        }
    }
}
