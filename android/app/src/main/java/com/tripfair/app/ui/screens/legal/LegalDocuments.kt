package com.tripfair.app.ui.screens.legal

import androidx.annotation.StringRes
import com.tripfair.app.R

/** One block inside a section card. */
sealed interface LegalBlock {
    /** Paragraph with an optional bold lead-in; [muted] greys out the body text. */
    data class Paragraph(@param:StringRes val lead: Int?, @param:StringRes val body: Int, val muted: Boolean) : LegalBlock

    /** Small grey note at the end of a card. */
    data class Note(@param:StringRes val text: Int) : LegalBlock

    /** Teal heading with a paragraph, separated by dividers (device permissions). */
    data class Permission(@param:StringRes val title: Int, @param:StringRes val body: Int) : LegalBlock
}

/** A numbered section; a non-null [warning] draws it as a red caution card with that heading. */
data class LegalSection(
    @param:StringRes val title: Int,
    val blocks: List<LegalBlock>,
    @param:StringRes val warning: Int? = null,
)

data class LegalDocument(
    @param:StringRes val title: Int,
    @param:StringRes val badge: Int,
    @param:StringRes val intro: Int,
    @param:StringRes val disclaimer: Int,
    val sections: List<LegalSection>,
)

private fun lead(lead: Int, body: Int, muted: Boolean = true) = LegalBlock.Paragraph(lead, body, muted)
private fun text(body: Int, muted: Boolean = true) = LegalBlock.Paragraph(null, body, muted)
private fun note(text: Int) = LegalBlock.Note(text)

val PrivacyPolicy = LegalDocument(
    title = R.string.privacy_title,
    badge = R.string.privacy_badge,
    intro = R.string.privacy_intro,
    disclaimer = R.string.privacy_disclaimer,
    sections = listOf(
        LegalSection(
            R.string.privacy_s1,
            listOf(
                lead(R.string.privacy_s1_account_lead, R.string.privacy_s1_account),
                lead(R.string.privacy_s1_trip_lead, R.string.privacy_s1_trip),
                lead(R.string.privacy_s1_safety_lead, R.string.privacy_s1_safety),
            ),
        ),
        LegalSection(R.string.privacy_s2, listOf(text(R.string.privacy_s2_p1), text(R.string.privacy_s2_p2))),
        LegalSection(
            R.string.privacy_s3,
            listOf(
                lead(R.string.privacy_s3_group_lead, R.string.privacy_s3_group, muted = false),
                lead(R.string.privacy_s3_vendors_lead, R.string.privacy_s3_vendors, muted = false),
                lead(R.string.privacy_s3_sos_lead, R.string.privacy_s3_sos),
                note(R.string.privacy_s3_note),
            ),
        ),
        LegalSection(
            R.string.privacy_s4,
            listOf(
                LegalBlock.Permission(R.string.privacy_s4_camera, R.string.privacy_s4_camera_text),
                LegalBlock.Permission(R.string.privacy_s4_gps, R.string.privacy_s4_gps_text),
                LegalBlock.Permission(R.string.privacy_s4_sms, R.string.privacy_s4_sms_text),
                note(R.string.privacy_s4_note),
            ),
        ),
        LegalSection(
            R.string.privacy_s5,
            listOf(text(R.string.privacy_s5_p1, muted = false), text(R.string.privacy_s5_p2)),
            warning = R.string.privacy_s5_warning,
        ),
        LegalSection(
            R.string.privacy_s6,
            listOf(
                lead(R.string.privacy_s6_permissions_lead, R.string.privacy_s6_permissions),
                lead(R.string.privacy_s6_manage_lead, R.string.privacy_s6_manage),
                lead(R.string.privacy_s6_delete_lead, R.string.privacy_s6_delete),
                note(R.string.privacy_s6_note),
            ),
        ),
        LegalSection(R.string.privacy_s7, listOf(text(R.string.privacy_s7_p1), note(R.string.privacy_s7_note))),
    ),
)

val TermsOfUse = LegalDocument(
    title = R.string.terms_title,
    badge = R.string.terms_badge,
    intro = R.string.terms_intro,
    disclaimer = R.string.terms_disclaimer,
    sections = listOf(
        LegalSection(
            R.string.terms_s1,
            listOf(
                lead(R.string.terms_s1_what_lead, R.string.terms_s1_what, muted = false),
                lead(R.string.terms_s1_booking_lead, R.string.terms_s1_booking, muted = false),
                lead(R.string.terms_s1_student_lead, R.string.terms_s1_student, muted = false),
            ),
        ),
        LegalSection(
            R.string.terms_s2,
            listOf(
                lead(R.string.terms_s2_reference_lead, R.string.terms_s2_reference, muted = false),
                lead(R.string.terms_s2_estimate_lead, R.string.terms_s2_estimate, muted = false),
            ),
        ),
        LegalSection(
            R.string.terms_s3,
            listOf(
                lead(R.string.terms_s3_account_lead, R.string.terms_s3_account, muted = false),
                lead(R.string.terms_s3_group_lead, R.string.terms_s3_group, muted = false),
                lead(R.string.terms_s3_content_lead, R.string.terms_s3_content, muted = false),
                note(R.string.terms_s3_note),
            ),
        ),
        LegalSection(
            R.string.terms_s4,
            listOf(text(R.string.terms_s4_p1, muted = false), text(R.string.terms_s4_p2)),
            warning = R.string.terms_s4_warning,
        ),
        LegalSection(
            R.string.terms_s5,
            listOf(
                lead(R.string.terms_s5_update_lead, R.string.terms_s5_update, muted = false),
                lead(R.string.terms_s5_stop_lead, R.string.terms_s5_stop, muted = false),
                lead(R.string.terms_s5_law_lead, R.string.terms_s5_law, muted = false),
                note(R.string.terms_s5_note),
            ),
        ),
        LegalSection(R.string.terms_s6, listOf(text(R.string.terms_s6_p1), note(R.string.terms_s6_note))),
    ),
)
