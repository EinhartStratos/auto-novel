package infra.web.repository

import com.mongodb.client.model.Aggregates.*
import com.mongodb.client.model.Facet
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Projections.*
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Sorts.descending
import infra.*
import infra.common.Page
import infra.common.emptyPage
import infra.web.WebNovel
import infra.web.WebNovelListItem
import infra.web.WebNovelReadHistoryDbModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

class WebNovelReadHistoryRepository(
    mongo: MongoClient,
) {
    private val userReadHistoryWebCollection =
        mongo.database.getCollection<WebNovelReadHistoryDbModel>(
            MongoCollectionNames.WEB_READ_HISTORY,
        )

    suspend fun listReaderHistory(
        userId: String,
        page: Int,
        pageSize: Int,
    ): Page<WebNovelListItem> {
        @Serializable
        data class WebNovelAndHistory(
            @Contextual val createAt: Instant,
            val novel: WebNovel,
        )

        @Serializable
        data class PageModel(
            val total: Int = 0,
            val items: List<WebNovelAndHistory>,
        )

        val doc = userReadHistoryWebCollection
            .aggregate<PageModel>(
                match(eq(WebNovelReadHistoryDbModel::userId.field(), ObjectId(userId))),
                sort(
                    descending(WebNovelReadHistoryDbModel::createAt.field()),
                ),
                facet(
                    Facet("count", count()),
                    Facet(
                        "items",
                        skip(page * pageSize),
                        limit(pageSize),
                        lookup(
                            /* from = */ MongoCollectionNames.WEB_NOVEL,
                            /* localField = */ WebNovelReadHistoryDbModel::novelId.field(),
                            /* foreignField = */ WebNovel::id.field(),
                            /* as = */ "novel"
                        ),
                        unwind("\$novel"),
                    )
                ),
                project(
                    fields(
                        computed(PageModel::total.field(), arrayElemAt("count.count", 0)),
                        include(PageModel::items.field())
                    )
                ),
            )
            .firstOrNull()
        return if (doc == null) {
            emptyPage()
        } else {
            Page(
                items = doc.items.map { it.novel.toOutline().copy(lastReadAt = it.createAt) },
                total = doc.total.toLong(),
                pageSize = pageSize,
            )
        }
    }

    suspend fun deleteReadHistoryByUser(
        userId: String,
    ) {
        userReadHistoryWebCollection
            .deleteMany(
                and(
                    eq(WebNovelReadHistoryDbModel::userId.field(), ObjectId(userId)),
                ),
            )
    }

    suspend fun getReaderHistory(
        userId: String,
        novelId: String,
    ): WebNovelReadHistoryDbModel? {
        return userReadHistoryWebCollection
            .find(
                and(
                    eq(WebNovelReadHistoryDbModel::userId.field(), ObjectId(userId)),
                    eq(WebNovelReadHistoryDbModel::novelId.field(), ObjectId(novelId)),
                ),
            )
            .firstOrNull()
    }

    suspend fun updateReadHistory(
        userId: String,
        novelId: String,
        chapterId: String,
    ) {
        userReadHistoryWebCollection
            .replaceOne(
                and(
                    eq(WebNovelReadHistoryDbModel::userId.field(), ObjectId(userId)),
                    eq(WebNovelReadHistoryDbModel::novelId.field(), ObjectId(novelId)),
                ),
                WebNovelReadHistoryDbModel(
                    userId = ObjectId(userId),
                    novelId = ObjectId(novelId),
                    chapterId = chapterId,
                    createAt = Clock.System.now(),
                ),
                ReplaceOptions().upsert(true),
            )
    }

    suspend fun deleteReadHistory(
        userId: String,
        novelId: String,
    ) {
        userReadHistoryWebCollection
            .deleteOne(
                and(
                    eq(WebNovelReadHistoryDbModel::userId.field(), ObjectId(userId)),
                    eq(WebNovelReadHistoryDbModel::novelId.field(), ObjectId(novelId)),
                ),
            )
    }
}
