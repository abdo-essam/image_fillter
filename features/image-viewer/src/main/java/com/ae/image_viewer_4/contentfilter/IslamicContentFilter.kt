package com.ae.image_viewer_4.contentfilter

import android.graphics.Bitmap
import com.ae.image_viewer_4.contentfilter.models.ContentClassification
import com.google.mlkit.vision.face.Face

internal class IslamicContentFilter {

    // Labels that indicate female presence
    private val femaleIndicatorLabels = setOf(
        "woman", "women", "girl", "girls", "female", "lady", "ladies",
        "daughter", "mother", "sister", "aunt", "grandmother",
        "bride", "wife", "she", "her", "feminine"
    )

    // Additional context labels that might indicate female presence
    private val femaleContextLabels = setOf(
        "dress", "skirt", "makeup", "lipstick", "jewelry", "earring",
        "necklace", "handbag", "purse", "heels", "feminine clothing"
    )

    suspend fun shouldFilterContent(analysis: ContentDetector.ContentAnalysis): FilterResult {
        // Check if any female is detected in the image
        val femaleDetected = detectFemalePresence(analysis)
        if (femaleDetected != null) {
            return FilterResult.Filtered(femaleDetected)
        }

        // Check faces for female characteristics
        val femaleFaceDetected = detectFemaleFaces(analysis)
        if (femaleFaceDetected != null) {
            return FilterResult.Filtered(femaleFaceDetected)
        }

        // Additional content checks (alcohol, pork, etc.)
        val inappropriateContent = checkOtherInappropriateContent(analysis.labels)
        if (inappropriateContent != null) {
            return FilterResult.Filtered(inappropriateContent)
        }

        return FilterResult.Allowed
    }

    private fun detectFemalePresence(analysis: ContentDetector.ContentAnalysis): String? {
        // Check primary female indicators with lower confidence threshold
        for (label in analysis.labels) {
            val labelLower = label.text.lowercase()

            // Direct female indicators - filter with any confidence
            for (indicator in femaleIndicatorLabels) {
                if (labelLower.contains(indicator) && label.confidence > 0.3f) {
                    return "Female presence detected: ${label.text}"
                }
            }

            // Context indicators with higher threshold
            for (context in femaleContextLabels) {
                if (labelLower.contains(context) && label.confidence > 0.5f) {
                    return "Female-related content detected: ${label.text}"
                }
            }
        }

        // Check combinations of labels that might indicate female presence
        val hasPersonLabel = analysis.labels.any {
            it.text.lowercase().contains("person") && it.confidence > 0.5f
        }

        val hasFemaleContext = analysis.labels.any { label ->
            femaleContextLabels.any { context ->
                label.text.lowercase().contains(context)
            }
        }

        if (hasPersonLabel && hasFemaleContext) {
            return "Likely female presence detected based on context"
        }

        return null
    }

    private fun detectFemaleFaces(analysis: ContentDetector.ContentAnalysis): String? {
        if (analysis.faces.isEmpty()) return null

        // If we have faces but no clear male indicators, assume female
        val hasMaleIndicators = analysis.labels.any { label ->
            val text = label.text.lowercase()
            (text.contains("man") || text.contains("men") ||
                    text.contains("boy") || text.contains("male") ||
                    text.contains("beard") || text.contains("mustache")) &&
                    !text.contains("woman") && !text.contains("female")
        }

        // If faces detected and no clear male indicators, filter as potentially female
        if (analysis.faces.isNotEmpty() && !hasMaleIndicators) {
            // Check if we have any gender-neutral person labels
            val hasPersonLabel = analysis.labels.any { label ->
                val text = label.text.lowercase()
                text.contains("person") || text.contains("face") ||
                        text.contains("portrait") || text.contains("human")
            }

            if (hasPersonLabel) {
                return "Unverified person detected - filtering for safety"
            }
        }

        return null
    }

    private fun checkOtherInappropriateContent(labels: List<ContentDetector.ImageLabel>): String? {
        for (label in labels) {
            if (label.confidence > 0.6f) {
                val text = label.text.lowercase()
                when {
                    text.contains("alcohol") || text.contains("wine") ||
                            text.contains("beer") || text.contains("liquor") ->
                        return "Alcohol content detected"

                    text.contains("pork") || text.contains("bacon") ||
                            text.contains("ham") || text.contains("pig") ->
                        return "Haram food detected"

                    text.contains("gambling") || text.contains("casino") ||
                            text.contains("betting") || text.contains("lottery") ->
                        return "Gambling content detected"

                    text.contains("bikini") || text.contains("swimsuit") ||
                            text.contains("lingerie") || text.contains("underwear") ->
                        return "Inappropriate clothing detected"
                }
            }
        }
        return null
    }

    sealed class FilterResult {
        object Allowed : FilterResult()
        data class Filtered(val reason: String) : FilterResult()
    }
}