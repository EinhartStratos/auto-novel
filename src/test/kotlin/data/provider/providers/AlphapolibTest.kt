package data.provider.providers

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith

class AlphapolibTest : DescribeSpec({
    val provider = Alphapolib()

    describe("getMetadata") {
        it("常规") {
            // https://www.alphapolis.co.jp/novel/638978238/525733370
            val metadata = provider.getMetadata("638978238-525733370")
            metadata.title.shouldStartWith("今までの功績を改竄され")
            metadata.authors.first().name.shouldBe("taki210")
            metadata.authors.first().link.shouldBe("https://www.alphapolis.co.jp/author/detail/638978238")
            metadata.introduction.shouldStartWith("「今日限りでお前をこの")
            metadata.introduction.shouldEndWith("っていたのだった。")
            metadata.toc[0].title.shouldBe("第一話")
            metadata.toc[0].episodeId.shouldBe("6857738")
        }
    }

    describe("getEpisode") {
        it("常规") {
            // https://www.alphapolis.co.jp/novel/638978238/525733370/episode/6857739
            val episode = provider.getEpisode("638978238-525733370", "6857739")
        }
    }
})