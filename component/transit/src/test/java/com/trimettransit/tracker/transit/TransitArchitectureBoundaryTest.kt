package com.trimettransit.tracker.transit

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Compile-grep lock for the repository boundary (T7).
 *
 * The brief's Robolectric `repositoryHoldsApplicationContext` test cannot run here:
 * repo convention is plain JUnit4 only (no Robolectric/Mockito; `component:transit`
 * test deps are junit + org.json + joda). The `applicationContext` hold and the
 * `internal` visibility are therefore asserted via source inspection, following the
 * same `File("src/...")` working-dir convention as [LiveTripPlanResponseParserTest].
 */
class TransitArchitectureBoundaryTest {

    private fun mainSource(name: String): String =
        File("src/main/java/com/trimettransit/tracker/transit/$name").readText()

    @Test fun repositoryHoldsApplicationContext() {
        val src = mainSource("TransitRepositoryImpl.kt")
        assertTrue(
            "TransitRepositoryImpl must retain context.applicationContext, not the raw caller Context",
            src.contains("context.applicationContext")
        )
    }

    @Test fun networkSingletonsAreModuleInternal() {
        val internals = mapOf(
            "TransitApi.kt" to "internal object TransitApi",
            "JSONParser.kt" to "internal object JSONParser",
            "TransitJsonMapper.kt" to "internal object TransitJsonMapper",
            "TripPlannerXmlParser.kt" to "internal object TripPlannerXmlParser",
            "ApiKeys.kt" to "internal object ApiKeys"
        )
        for ((file, decl) in internals) {
            assertTrue("$file must declare `$decl`", mainSource(file).contains(decl))
        }
        assertTrue(
            "TripPlanFailureClassifier.kt must declare `internal class HttpResponseCodeException`",
            mainSource("TripPlanFailureClassifier.kt")
                .contains("internal class HttpResponseCodeException")
        )
    }

    @Test fun featuresDoNotImportTransitInternals() {
        // Feature modules may depend only on the TransitRepository interface from
        // common:model; reaching into component:transit's network singletons breaks
        // the app -> feature -> component -> common layer boundary.
        val banned = listOf(
            "TransitApi",
            "JSONParser",
            "TransitJsonMapper",
            "TripPlannerXmlParser",
            "ApiKeys",
            "HttpResponseCodeException"
        )
        val featureRoot = File("../../feature")
        assertTrue(
            "expected repo-checkout layout (../../feature is ${featureRoot.absolutePath})",
            featureRoot.isDirectory
        )
        val violations = featureRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { idx, line ->
                    val trimmed = line.trim()
                    val hit = banned.firstOrNull { sym ->
                        val prefix = "import com.trimettransit.tracker.transit.$sym"
                        trimmed.startsWith(prefix) &&
                            (trimmed.length == prefix.length ||
                                !trimmed[prefix.length].isLetterOrDigit())
                    }
                    hit?.let { "${file.path}:${idx + 1} imports $hit" }
                }
            }.toList()
        assertTrue(
            "feature modules must not import transit internals: $violations",
            violations.isEmpty()
        )
    }
}
