package org.mpdx.android.core.data.realm

import io.realm.DynamicRealm
import io.realm.DynamicRealmObject
import io.realm.FieldAttribute
import io.realm.RealmList
import io.realm.RealmMigration
import io.realm.RealmObjectSchema
import io.realm.kotlin.oneOf
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import org.ccci.gto.android.common.util.localizedToDoubleOrNull
import org.mpdx.android.base.realm.PACKED_FIELD_SEPARATOR
import org.mpdx.android.features.AppPrefs
import org.mpdx.android.features.contacts.model.Contact
import org.mpdx.android.features.filter.model.Filter
import org.mpdx.android.utils.dayOfYear
import org.mpdx.android.utils.monthDayOrNull
import org.mpdx.android.utils.toDate
import org.mpdx.android.utils.toInstanOrNull
import org.mpdx.android.utils.toLocalDateOrNull
import org.mpdx.android.utils.toRealmList

/**
 * Version History
 * v2.10.0
 *  51: 2019-04-02
 *  52: 2019-04-09
 *  53: 2019-04-09
 *  54: 2019-04-10
 *  55: 2019-04-10
 *  56: 2019-04-10
 * v2.10.1
 *  57: 2019-04-10
 *  58: 2019-04-10
 *  59: 2019-04-11
 *  60: 2019-04-11
 *  61: 2019-04-12
 *  62: 2019-04-15
 *  63: 2019-04-16
 *  64: 2019-04-15
 *  65: 2019-04-15
 *  66: 2019-04-15
 *  67: 2019-04-12
 *  68: 2019-04-12
 *  69: 2019-04-12
 *  70: 2019-04-12
 *  71: 2019-04-12
 *  72: 2019-04-12
 *  73: 2019-04-18
 *  74: 2019-04-18
 *  75: 2019-04-16
 *  76: 2019-04-17
 *  77: 2019-04-18
 *  78: 2019-04-24
 *  79: 2019-04-25
 *  80: 2019-04-25
 *  81: 2019-04-25
 *  82: 2019-04-25
 *  83: 2019-04-30
 *  84: 2019-04-30
 *  85: 2019-04-29
 *  86: 2019-05-01
 *  87: 2019-05-03
 *  88: 2019-05-03
 *  89: 2019-05-06
 *  90: 2019-05-06
 *  91: 2019-05-06
 *  92: 2019-05-13
 *  93: 2019-05-17
 *  94: 2019-05-17
 *  95: 2019-05-21
 *  96: 2019-05-22
 *  97: 2019-05-22
 *  98: 2019-05-28
 *  99: 2019-05-29
 * 100: 2019-05-29
 * 101: 2019-05-06
 * 102: 2019-05-31
 * 103: 2019-06-03
 * 104: 2019-05-31
 * 105: 2019-06-07
 * 106: 2019-06-10
 * 107: 2019-06-10
 * 108: 2019-06-10
 * 109: 2019-06-12
 * 110: 2019-06-14
 * 111: 2019-06-14
 * 112: 2019-06-17
 * 113: 2019-06-21
 * 114: 2019-06-21
 * 115: 2019-06-21
 * 116: 2019-06-24
 * 117: 2019-06-24
 * 118: 2019-06-24
 * 119: 2019-05-17
 * 120: 2019-05-17
 * 121: 2019-06-27
 * 122: 2019-06-28
 * 123: 2019-06-28
 * 124: 2019-07-03
 * 125: 2019-07-05
 * 126: 2019-07-05
 * 127: 2019-07-05
 * 128: 2019-07-02
 * v2.10.2
 * 129: 2019-07-24
 * 130: 2019-07-26
 * 131: 2019-07-26
 * 132: 2019-07-26
 * 133: 2019-07-30
 * 134: 2019-07-31
 * 135: 2019-08-06
 * 136: 2019-08-06
 * 137: 2019-08-06
 * 138: 2019-08-06
 * 139: 2019-08-05
 * 140: 2019-07-26
 * 141: 2019-08-08
 * 142: 2019-02-28
 * 143: 2019-08-14
 * 144: 2019-08-14
 * 145: 2019-08-14
 * 146: 2019-08-15
 * 147: 2019-08-16
 * v2.10.3
 * 148: 2019-08-30
 * 149: 2019-08-30
 * 150: 2019-09-04
 * 151: 2019-09-04
 * 152: 2019-09-06
 * 153: 2019-09-10
 * 154: 2019-09-11
 * 155: 2019-09-20
 * 156: 2019-09-24
 * 157: 2019-09-24
 * 158: 2019-09-25
 * 159: 2019-09-25
 * 160: 2019-09-26
 * 161: 2019-10-02
 * 162: 2020-06-22
 * 163: 2020-06-22
 * 164: 2020-06-22
 * 165: 2020-07-06
 * 166: 2020-07-20
 * 167: 2021-01-11
 */
@Singleton
@Suppress("NOTHING_TO_INLINE")
class MpdxRealmMigration @Inject constructor(private val appPrefs: AppPrefs) : RealmMigration {
    companion object {
        const val VERSION = 167L
    }

    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {
        // perform upgrade in increments
        for (upgradeTo in oldVersion + 1..newVersion) {
            when (upgradeTo) {
                51L -> realm.schema.remove("AccountListCoaches")
                52L -> realm.schema.rename("DesignationAccounts", "DesignationAccount")
                53L -> realm.removeDesignationAccountUnusedFields()
                54L -> realm.schema.remove("RealmDate")
                55L -> realm.removeUnusedCoachingAnalytics()
                56L -> realm.removeDBItemFieldsFromCoachingAnalytics()
                57L -> realm.renamePhoneNumberBooleanFields()
                58L -> realm.renameEmailAddressBooleanFields()
                59L -> realm.schema.rename("People", "Person")
                60L -> realm.updatePersonDateFieldTypes()
                61L -> realm.addPersonBirthdayDayOfYear()
                62L -> realm.schema["NotificationPreference"]!!.addChangeAwareFields()
                63L -> realm.addAccountListIdToNotificationPreference()
                64L -> realm.schema.rename("Addresses", "Address")
                65L -> realm.renameAddressHistoricField()
                66L -> realm.removeUnusedAddressField()
                67L -> realm.addLocalAttributesToCoachingAnalytics()
                68L -> realm.addIdToCoachingAnalyticsStat("CoachingStatAppointments")
                69L -> realm.addIdToCoachingAnalyticsStat("CoachingStatContacts")
                70L -> realm.addIdToCoachingAnalyticsStat("CoachingStatCorrespondence")
                71L -> realm.addIdToCoachingAnalyticsStat("CoachingStatElectronic")
                72L -> realm.addIdToCoachingAnalyticsStat("CoachingStatPhone")
                73L -> realm.schema["Contact"]!!.addChangeAwareFields()
                74L -> realm.schema["Address"]!!.addChangeAwareFields()
                75L -> realm.addPersonAnniversaryDayOfYear()
                76L -> realm.schema.rename("DonorAccounts", "DonorAccount")
                77L -> realm.addAddressContactField()
                78L -> realm.removeContactAddresesCollection()
                79L -> realm.removeUnusedAppointmentResultsFields()
                80L -> realm.schema.remove("CoachingStatMonthlyGained")
                81L -> realm.schema.remove("CoachingStatMonthlyLost")
                82L -> realm.schema.remove("CoachingStatSpecialGained")
                83L -> realm.changeDonationConvertedAmountToDouble()
                84L -> realm.addDonorAccountAccountList()
                85L -> realm.schema["Pledge"]!!.convertPledgeDonationIdToListOfIds()
                86L -> realm.supportAppointmentResultsIdGeneration()
                87L -> realm.addPersonContactRelationship()
                88L -> realm.removeContactPersonsCollection()
                89L -> realm.schema["EmailAddress"]!!.addChangeAwareFields()
                90L -> realm.schema["PhoneNumber"]!!.addChangeAwareFields()
                91L -> realm.schema["Person"]!!.addChangeAwareFields()
                92L -> realm.schema["Task"]!!.removeField("noDate")
                93L -> realm.makeContactPledgeReceivedOptional()
                94L -> realm.makeContactNoAppealsOptional()
                95L -> realm.addTaskAnalyticsAccountListId()
                96L -> realm.makeEmailAddressIsPrimaryOptional()
                97L -> realm.makePhoneNumberPrimaryOptional()
                98L -> realm.addAddressPrimaryField()
                99L -> realm.addPhoneNumberPersonRelationship()
                100L -> realm.removePersonPhoneNumbersCollection()
                101L -> realm.addEmailAddressPersonRelationship()
                102L -> realm.removePersonEmailAddressesCollection()
                103L -> realm.addTagContactTagForField()
                104L -> realm.renameContactTagsField()
                105L -> realm.addContactAccountListRelationship()
                106L -> realm.schema.rename("ContactsAnalytics", "ContactAnalytics")
                107L -> realm.removeContactAnalyticsUnusedFields()
                108L -> realm.addContactAnalyticsAccountListId()
                109L -> realm.schema.remove("SyncFailureModels")
                110L -> Unit
                111L -> realm.createExcludedContactSchema()
                112L -> realm.schema["AskedContact"]!!.addAskedContactForceListDeletionField()
                113L -> realm.removePersonContactIdField()
                114L -> realm.addAskedContactContactRelationship()
                115L -> realm.removeAskedContactContactFields()
                116L -> realm.addContactReferredByRelationship()
                117L -> realm.removeFiltersMatching(Filter.Type.CONTACT_REFERRER)
                118L -> realm.schema["Contact"]!!.removeField("contactsThatReferredMe")
                119L -> realm.addContactStarredAtField()
                120L -> realm.addContactIsStarredField()
                121L -> realm.addDesignationAccountAccountListField()
                122L -> realm.convertDonationDonorAccountToRelationship()
                123L -> realm.changeDonationAmountToDouble()
                124L -> realm.schema["Contact"]!!.renameField("donationLateAt", "donation_late_at")
                125L -> realm.addDonorAccountContactRelationship()
                126L -> realm.schema["Donation"]!!.removeField("contactId")
                127L -> realm.changeDonationConvertedAppealAmountToDouble()
                128L -> realm.createSocialMediaSchemas()
                129L -> realm.addContactTagsIndex()
                130L -> realm.removeUserUnusedFields()
                131L -> realm.removeUserPreferenceFields()
                132L -> realm.addCommentPersonRelationship()
                133L -> realm.convertTaskCompletedAtToDate()
                134L -> realm.convertPledgeAmountToDouble()
                135L -> realm.addContactPlaceholderField()
                136L -> realm.addAppealAccountListRelationship()
                137L -> realm.convertPledgeAppealRelationship()
                138L -> realm.convertPledgeContactRelationship()
                139L -> realm.convertNotificationContactToRelationship()
                140L -> realm.createTaskContactSchema()
                141L -> realm.removeTaskContactIds()
                142L -> realm.addDonationAccountListRelationship()
                143L -> realm.addTaskAccountListRelationship()
                144L -> realm.convertFilterToKotlin()
                145L -> realm.schema["Filter"]!!.renameField("type", "_type")
                146L -> realm.addFilterLabelField()
                147L -> realm.convertAccountListFieldsToDoubles()
                148L -> realm.removeAppealUnusedFields()
                149L -> realm.convertAppealFieldsToDoubles()
                150L -> realm.addDonationPledgeRelationship()
                151L -> realm.schema["Pledge"]!!.removeField("donationIds")
                152L -> realm.addPledgeAccountListRelationship()
                153L -> realm.clearTaskHiddenSubjects()
                154L -> realm.schema["Task"]!!.renameField("tagList", "tags")
                155L -> realm.schema["Task"]!!.renameField("notificationType", "notificationTypeValue")
                156L -> realm.schema["Task"]!!.renameField("notificationTimeUnit", "notificationTimeUnitValue")
                157L -> realm.schema["Task"]!!.renameField("completedAt", "completedAtValue")
                158L -> realm.schema["Task"]!!.renameField("result", "resultValue")
                159L -> realm.schema["Task"]!!.removeField("emailAddressIds")
                160L -> realm.schema["Task"]!!.removeField("phoneNumberIds")
                161L -> realm.resetFilters()
                162L -> realm.schema.rename("User", "DbUser")
                163L -> realm.addPersonPlaceholderField()
                164L -> realm.createUserModel()
                165L -> realm.addUserAccountListRelationship()
                166L -> realm.addTaskUserRelationship()
                167L -> realm.schema.remove("ContactAnalytics")
                else -> throw UnsupportedOperationException("Unsupported Database Migration")
            }
        }
    }

    // region JsonApiModel
    private fun RealmObjectSchema.addJsonApiModelPlaceholderField() {
        addField("isPlaceholder", Boolean::class.java, FieldAttribute.REQUIRED)
        transform { it.setBoolean("isPlaceholder", false) }
    }
    // endregion JsonApiModel

    // region ChangeAwareItem
    private fun RealmObjectSchema.addChangeAwareFields() = apply {
        addIsNewAndIsDeletedFlags()
        addChangedFieldsField()
    }

    private fun RealmObjectSchema.addIsNewAndIsDeletedFlags() {
        addField("isNew", Boolean::class.java)
        addField("isDeleted", Boolean::class.java)
        transform {
            it.setBoolean("isNew", false)
            it.setBoolean("isDeleted", false)
        }
    }

    private fun RealmObjectSchema.addChangedFieldsField() {
        addField("changedFieldsStr", String::class.java, FieldAttribute.REQUIRED)
        transform {
            it.set("changedFieldsStr", "")
        }
    }
    // endregion ChangeAwareItem

    // region LastSyncTime
    private fun DynamicRealm.removeLastSyncFor(prefix: String) {
        where("LastSyncTime").contains("id", prefix).findAll().deleteAllFromRealm()
    }
    // endregion LastSyncTime

    // region AccountList migrations
    private inline fun DynamicRealm.convertAccountListFieldsToDoubles() = schema["AccountList"]!!.apply {
        convertFieldFromStringToDouble("balance")
        convertFieldFromStringToDouble("monthlyGoal")
        convertFieldFromStringToDouble("committed")
        convertFieldFromStringToDouble("received")

        removeLastSyncFor("account_lists")
        removeLastSyncFor("coaching_account_lists")
    }
    // endregion AccountList migrations

    // region Address migrations
    private fun DynamicRealm.renameAddressHistoricField() = schema["Address"]!!.renameField("historic", "isHistoric")

    private fun DynamicRealm.removeUnusedAddressField() = schema["Address"]!!.removeField("validValues")

    private fun DynamicRealm.addAddressContactField() {
        schema["Address"]!!.addRealmObjectField("contact", schema["Contact"]!!)
        schema["Contact"]!!.transform { contact ->
            contact.getList("addresses").forEach { address ->
                address.setObject("contact", contact)
            }
        }
    }

    private fun DynamicRealm.addAddressPrimaryField() = schema["Address"]!!.apply {
        addField("isPrimary", Boolean::class.java)
        setRequiredIfNeeded("isPrimary", false)
        transform { address ->
            address.setBoolean("isPrimary", address.getString("primaryMailingAddress") == "true")
        }
        removeField("primaryMailingAddress")
    }
    // endregion Address migrations

    // region Appeal migrations
    private inline fun DynamicRealm.addAppealAccountListRelationship() = schema["Appeal"]!!.apply {
        addRealmObjectField("accountList", schema["AccountList"]!!)
        val accountList = where("AccountList").equalTo("id", appPrefs.accountListId).findFirst()
        transform {
            it.setObject("accountList", accountList)
        }
        removeLastSyncFor("appeal")
    }

    private inline fun DynamicRealm.removeAppealUnusedFields() = schema["Appeal"]!!.apply {
        removeField("currencies")
        removeField("description")
        removeField("endDate")
        removeField("updatedAt")
        removeField("updatedInDatabaseAt")
    }

    private inline fun DynamicRealm.convertAppealFieldsToDoubles() = schema["Appeal"]!!.apply {
        convertFieldFromStringToDouble("amount")
        convertFieldFromStringToDouble("pledgesAmountNotReceivedNotProcessed")
        convertFieldFromStringToDouble("pledgesAmountReceivedNotProcessed")
        convertFieldFromStringToDouble("pledgesAmountProcessed")
        convertFieldFromStringToDouble("pledgesAmountTotal")
        renameField("pledgesAmountNotReceivedNotProcessed", "pledgesNotReceived")
        renameField("pledgesAmountReceivedNotProcessed", "pledgesReceivedNotProcessed")
        renameField("pledgesAmountProcessed", "pledgesProcessed")
        renameField("pledgesAmountTotal", "pledgesTotal")
        renameField("totalCurrency", "currency")
        removeLastSyncFor("appeal")
    }
    // endregion Appeal migrations

    // region AskedContact migrations
    private inline fun RealmObjectSchema.addAskedContactForceListDeletionField() {
        addField("forceListDeletion", Boolean::class.java)
        apply {
            transform { askedContact ->
                askedContact.setBoolean("forceListDeletion", false)
            }
        }
    }

    private inline fun DynamicRealm.addAskedContactContactRelationship() = schema["AskedContact"]!!.apply {
        addRealmObjectField("contact", schema["Contact"]!!)
        transform {
            it.setObject("contact", where("Contact").equalTo("id", it.getString("contactId")).findFirst())
        }
    }

    private inline fun DynamicRealm.removeAskedContactContactFields() = schema["AskedContact"]!!.apply {
        removeField("contactId")
        removeField("name")
    }
    // endregion AskedContact migrations

    // region CoachingAnalytics

    private inline fun DynamicRealm.removeUnusedCoachingAnalytics() {
        schema["CoachingAnalytics"]!!.apply {
            removeField("emailStat")
            removeField("facebookStat")
            removeField("textMessageStat")
        }
        schema.apply {
            remove("CoachingStatEmail")
            remove("CoachingStatFacebook")
            remove("CoachingStatTextMessage")
        }
    }

    private inline fun DynamicRealm.removeDBItemFieldsFromCoachingAnalytics() = schema["CoachingAnalytics"]!!.apply {
        removeField("createdAt")
        removeField("updatedAt")
        removeField("updatedInDatabaseAt")
    }

    private inline fun DynamicRealm.addLocalAttributesToCoachingAnalytics() = schema["CoachingAnalytics"]!!.apply {
        addField("accountListId", String::class.java)
        addField("_startDate", String::class.java)
        addField("_endDate", String::class.java)
        removeField("startDate")
        removeField("endDate")
        where("CoachingAnalytics").findAll().deleteAllFromRealm()
    }

    // region Stats
    private fun DynamicRealm.addIdToCoachingAnalyticsStat(className: String) = schema[className]!!.apply {
        where(className).findAll().deleteAllFromRealm()
        addField("id", String::class.java)
        addPrimaryKey("id")
    }
    // endregion Stats
    // endregion CoachingAnalytics

    // region CoachingAppointmentResults

    private inline fun DynamicRealm.removeUnusedAppointmentResultsFields() =
        schema["CoachingAppointmentResults"]!!.apply {
            removeField("createdAt")
            removeField("updatedAt")
            removeField("updatedInDatabaseAt")
            removeField("monthlyGainedStat")
            removeField("monthlyLostStat")
            removeField("specialGainedStat")
        }

    private inline fun DynamicRealm.supportAppointmentResultsIdGeneration() =
        schema["CoachingAppointmentResults"]!!.apply {
            removeField("startDate")
            removeField("endDate")
            addField("startDate", Long::class.java)
            setRequiredIfNeeded("startDate", false)
            addField("endDate", Long::class.java)
            setRequiredIfNeeded("endDate", false)
            addField("accountListId", String::class.java)
            setRequiredIfNeeded("accountListId", false)
            where("CoachingAppointmentResults").findAll().deleteAllFromRealm()
        }

    // endregion CoachingAppointmentResults

    // region Comment migrations
    private inline fun DynamicRealm.addCommentPersonRelationship() = schema["Comment"]!!.apply {
        addRealmObjectField("person", schema["Person"]!!)
        transform {
            val id = it.getString("personId")
            val person = where("Person").equalTo("id", id).findFirst()
                ?: if (id != null) createObject("Person", id) else null
            it.setObject("person", person)
        }
        removeField("personId")
    }
    // endregion Comment migrations

    // region Contact migrations
    private inline fun DynamicRealm.removeContactAddresesCollection() = schema["Contact"]!!.removeField("addresses")
    private inline fun DynamicRealm.removeContactPersonsCollection() = schema["Contact"]!!.removeField("persons")
    private inline fun DynamicRealm.makeContactPledgeReceivedOptional() =
        schema["Contact"]!!.setRequiredIfNeeded("pledgeReceived", false)

    private inline fun DynamicRealm.makeContactNoAppealsOptional() =
        schema["Contact"]!!.setRequiredIfNeeded("noAppeals", false)

    private inline fun DynamicRealm.renameContactTagsField() = schema["Contact"]!!.renameField("tagList", "tags")

    private inline fun DynamicRealm.addContactAccountListRelationship() = schema["Contact"]!!.apply {
        addRealmObjectField("accountList", schema["AccountList"]!!)
        setRequiredIfNeeded("accountList", false)
        transform {
            val accountListId = it.getString("accountListId") ?: appPrefs.accountListId
            val accountList = where("AccountList").equalTo("id", accountListId).findFirst()
                ?: if (accountListId != null) createObject("AccountList", accountListId) else null
            it.setObject("accountList", accountList)
        }
        removeField("accountListId")
    }

    private inline fun DynamicRealm.addContactReferredByRelationship() = schema["Contact"]!!.run {
        addRealmListField("referredBy", this)
        transform {
            val ids = it.getList("contactsThatReferredMe", String::class.java)
            it.setList(
                "referredBy",
                ids.filterNotNull()
                    .map { id -> where("Contact").equalTo("id", id).findFirst() ?: Contact().also { it.id = id } }
                    .toRealmList()
            )
        }
    }

    private inline fun DynamicRealm.addContactStarredAtField() {
        schema["Contact"]!!.addField("starredAt", Date::class.java)
        removeLastSyncFor("contact")
    }

    private inline fun DynamicRealm.addContactIsStarredField() {
        schema["Contact"]!!.addField("isStarred", Boolean::class.java)
        schema["Contact"]!!.transform {
            it.setBoolean("isStarred", it.getDate("starredAt") != null)
        }
    }

    private inline fun DynamicRealm.addContactTagsIndex() = schema["Contact"]!!.apply {
        addField("tagsIndex", String::class.java)
        transform {
            val tags = it.getList("tags", String::class.java).takeUnless { it.isEmpty() }
            it.set(
                "tagsIndex", tags?.joinToString(PACKED_FIELD_SEPARATOR, PACKED_FIELD_SEPARATOR, PACKED_FIELD_SEPARATOR)
            )
        }
    }

    private inline fun DynamicRealm.addContactPlaceholderField() = schema["Contact"]!!.apply {
        addJsonApiModelPlaceholderField()
        where("Contact").isNull("accountList").or().isNull("name").findAll().forEach {
            it.setBoolean("isPlaceholder", true)
        }
    }
    // endregion Contact migrations

    // region ContactAnalytics migrations
    private inline fun DynamicRealm.removeContactAnalyticsUnusedFields() = schema["ContactAnalytics"]!!.apply {
        removeField("createdAt")
        removeField("updatedAt")
        removeField("updatedInDbAt")
        removeField("birthdaysThisWeekIds")
        removeField("anniversariesThisWeek")
    }

    private inline fun DynamicRealm.addContactAnalyticsAccountListId() = schema["ContactAnalytics"]!!.apply {
        addField("accountListId", String::class.java)
        setRequiredIfNeeded("accountListId", false)
    }
    // endregion ContactAnalytics migrations

    // region DesignationAccount migrations
    private inline fun DynamicRealm.removeDesignationAccountUnusedFields() = schema["DesignationAccount"]!!.apply {
        removeField("createdAt")
        removeField("updatedAt")
        removeField("updatedInDatabaseAt")
    }

    private inline fun DynamicRealm.addDesignationAccountAccountListField() = schema["DesignationAccount"]!!.apply {
        addRealmObjectField("accountList", schema["AccountList"]!!)
        setRequiredIfNeeded("accountList", false)

        val accountList = where("AccountList").equalTo("id", appPrefs.accountListId).findFirst() ?: return@apply
        transform {
            it.setObject("accountList", accountList)
        }
    }
    // endregion DesignationAccount migrations

    // region Donation migrations
    private fun DynamicRealm.changeDonationConvertedAmountToDouble() {
        schema["Donation"]!!.apply {
            addField("newConvertedAmount", Double::class.java)
            setRequiredIfNeeded("newConvertedAmount", false)
            transform {
                it.getString("convertedAmount").toDoubleOrNull()?.let { amount ->
                    it.setDouble("newConvertedAmount", amount)
                }
            }
            removeField("convertedAmount")
            renameField("newConvertedAmount", "convertedAmount")
        }
        removeLastSyncFor("donations")
    }

    private fun DynamicRealm.convertDonationDonorAccountToRelationship() = schema["Donation"]!!.apply {
        addRealmObjectField("donorAccount", schema["DonorAccount"]!!)
        transform {
            it.setObject(
                "donorAccount", where("DonorAccount").equalTo("id", it.getString("donorAccountId")).findFirst()
            )
        }
        removeField("donorAccountId")
    }

    private fun DynamicRealm.changeDonationAmountToDouble() {
        schema["Donation"]!!.apply {
            addField("newAmount", Double::class.java)
            setRequiredIfNeeded("newAmount", false)
            transform {
                it.getString("amount").toDoubleOrNull()?.let { amount ->
                    it.setDouble("newAmount", amount)
                }
            }
            removeField("amount")
            renameField("newAmount", "amount")
        }
        removeLastSyncFor("donations")
    }

    private fun DynamicRealm.changeDonationConvertedAppealAmountToDouble() {
        schema["Donation"]!!.apply {
            addField("newAmount", Double::class.java)
            setRequiredIfNeeded("newAmount", false)
            transform {
                it.getString("convertedAppealAmount").toDoubleOrNull()?.let { amount ->
                    it.setDouble("newAmount", amount)
                }
            }
            removeField("convertedAppealAmount")
            renameField("newAmount", "convertedAppealAmount")
        }
        removeLastSyncFor("donations")
    }

    private fun DynamicRealm.addDonationAccountListRelationship() = schema["Donation"]!!.apply {
        addRealmObjectField("accountList", schema["AccountList"]!!)
        val accountList = where("AccountList").equalTo("id", appPrefs.accountListId).findFirst()
        transform {
            it.setObject("accountList", accountList)
        }
        removeLastSyncFor("donations")
    }

    private fun DynamicRealm.addDonationPledgeRelationship() = schema["Donation"]!!.apply {
        addRealmObjectField("pledge", schema["Pledge"]!!)
        where("Pledge").findAll().forEach { pledge ->
            pledge.getList("donationIds", String::class.java).forEach { donationId ->
                where("Donation").equalTo("id", donationId).findAll().forEach { it.setObject("pledge", pledge) }
            }
        }
        removeLastSyncFor("donations")
    }
    // endregion Donation migrations

    // region DonorAccount migrations
    private inline fun DynamicRealm.addDonorAccountAccountList() = schema["DonorAccount"]!!.apply {
        addRealmObjectField("accountList", schema["AccountList"]!!)
        setRequiredIfNeeded("accountList", false)

        val accountList = where("AccountList").equalTo("id", appPrefs.accountListId).findFirst() ?: return@apply
        transform {
            it.setObject("accountList", accountList)
        }
    }

    private inline fun DynamicRealm.addDonorAccountContactRelationship() = schema["DonorAccount"]!!.apply {
        addRealmListField("contacts", schema["Contact"]!!)

        transform {
            val contacts = it.getList("contactIds", String::class.java)
            it.setList("contacts", where("Contact").oneOf("id", contacts.toTypedArray()).findAll().toRealmList())
        }
        removeField("contactIds")

        removeLastSyncFor("donor_accounts")
    }
    // endregion DonorAccount migrations

    // region EmailAddress migrations

    private inline fun DynamicRealm.renameEmailAddressBooleanFields() = schema["EmailAddress"]!!.apply {
        renameField("primary", "isPrimary")
        renameField("historic", "isHistoric")
    }

    private inline fun DynamicRealm.makeEmailAddressIsPrimaryOptional() =
        schema["EmailAddress"]!!.setRequiredIfNeeded("isPrimary", false)

    private inline fun DynamicRealm.addEmailAddressPersonRelationship() {
        schema["EmailAddress"]!!.addRealmObjectField("person", schema["Person"]!!)
        schema["Person"]!!.transform { person ->
            person.getList("emailAddresses").forEach { email ->
                email.setObject("person", person)
            }
        }
    }
    // endregion EmailAddress migrations

    // region ExcludedAppealContact migrations
    private fun DynamicRealm.createExcludedContactSchema() = schema.create("ExcludedAppealContact").apply {
        addField("id", String::class.java)
        addPrimaryKey("id")
        addRealmObjectField("appeal", schema["Appeal"]!!)
        addRealmObjectField("contact", schema["Contact"]!!)
    }
    // endregion ExcludedAppealContact migrations

    // region Filter migrations
    private inline fun DynamicRealm.removeFiltersMatching(type: Filter.Type? = null) =
        where("Filter").apply { if (type != null) equalTo("type", type.ordinal) }.findAll().deleteAllFromRealm()

    private inline fun DynamicRealm.convertFilterToKotlin() = schema["Filter"]!!.apply {
        renameField("enabled", "isEnabled")
        setRequiredIfNeeded("container", false)
        setRequiredIfNeeded("type", false)
    }

    private inline fun DynamicRealm.addFilterLabelField() = schema["Filter"]!!.apply {
        addField("label", String::class.java)
        transform { f ->
            f.setString("label", f.getString("translatedLabel")?.takeIf { it.isNotEmpty() } ?: f.getString("key"))
        }
    }

    private inline fun DynamicRealm.resetFilters() = where("Filter").findAll().deleteAllFromRealm()
    // endregion Filter migrations

    // region Notification migrations
    private inline fun DynamicRealm.convertNotificationContactToRelationship() = schema["Notification"]!!.apply {
        addRealmObjectField("contact", schema["Contact"]!!)
        transform {
            val contact = where("Contact").equalTo("id", it.getString("contactId")).findFirst()
            it.setObject("contact", contact)
        }
        removeField("contactId")
        removeField("contactName")
        removeField("accountListId")

        removeLastSyncFor("notifications")
    }
    // endregion Notification migrations

    // region NotificationPreference migrations
    private inline fun DynamicRealm.addAccountListIdToNotificationPreference() =
        schema["NotificationPreference"]!!.apply {
            addField("accountListId", String::class.java)
            setRequiredIfNeeded("accountListId", false)
            transform {
                it.setString("accountListId", appPrefs.accountListId)
            }
        }
    // endregion NotificationPreference migrations

    // region Person migrations
    private inline fun DynamicRealm.updatePersonDateFieldTypes() = schema["Person"]!!.apply {
        listOf("birthdayYear", "birthdayMonth", "birthdayDay", "anniversaryYear", "anniversaryMonth", "anniversaryDay")
            .forEach { field ->
                addField("${field}_new", Integer::class.java)
                setRequiredIfNeeded("${field}_new", false)
                transform { row ->
                    row.getString(field)?.toIntOrNull()?.let { row.setInt("${field}_new", it) }
                }
                removeField(field)
                renameField("${field}_new", field)
            }
    }

    private inline fun DynamicRealm.addPersonBirthdayDayOfYear() = schema["Person"]!!.apply {
        addField("birthdayDayOfYear", Int::class.java)
        setRequiredIfNeeded("birthdayDayOfYear", false)
        transform { row ->
            row.setInt(
                "birthdayDayOfYear",
                monthDayOrNull(
                    row.getIntOrNull("birthdayMonth"),
                    row.getIntOrNull("birthdayDay")
                )?.dayOfYear
            )
        }
    }

    private inline fun DynamicRealm.addPersonAnniversaryDayOfYear() = schema["Person"]!!.apply {
        addField("anniversaryDayOfYear", Int::class.java)
        setRequiredIfNeeded("anniversaryDayOfYear", false)
        transform { row ->
            row.setInt(
                "anniversaryDayOfYear",
                monthDayOrNull(
                    row.getIntOrNull("anniversaryMonth"),
                    row.getIntOrNull("anniversaryDay")
                )?.dayOfYear
            )
        }
    }

    private inline fun DynamicRealm.addPersonContactRelationship() {
        schema["Person"]!!.addRealmObjectField("contact", schema["Contact"]!!)
        schema["Contact"]!!.transform { contact ->
            contact.getList("persons").forEach { address ->
                address.setObject("contact", contact)
            }
        }
    }

    private fun DynamicRealm.removePersonPhoneNumbersCollection() = schema["Person"]!!.removeField("phoneNumbers")
    private inline fun DynamicRealm.removePersonEmailAddressesCollection() =
        schema["Person"]!!.removeField("emailAddresses")

    private inline fun DynamicRealm.removePersonContactIdField() = schema["Person"]!!.apply {
        transform {
            if (it.getObject("contact") != null) return@transform
            it.setObject("contact", where("Contact").equalTo("id", it.getString("contactId")).findFirst())
        }
        removeField("contactId")
    }

    private inline fun DynamicRealm.addPersonPlaceholderField() = schema["Person"]!!.apply {
        addJsonApiModelPlaceholderField()
        where("Person").isNull("firstName").findAll().forEach {
            it.setBoolean("isPlaceholder", true)
        }
    }
    // endregion Person migrations

    // region PhoneNumber migrations

    private inline fun DynamicRealm.renamePhoneNumberBooleanFields() = schema["PhoneNumber"]!!.apply {
        renameField("primary", "isPrimary")
        renameField("historic", "isHistoric")
    }

    private inline fun DynamicRealm.makePhoneNumberPrimaryOptional() =
        schema["PhoneNumber"]!!.setRequiredIfNeeded("isPrimary", false)

    private inline fun DynamicRealm.addPhoneNumberPersonRelationship() {
        schema["PhoneNumber"]!!.addRealmObjectField("person", schema["Person"]!!)
        schema["Person"]!!.transform { person ->
            person.getList("phoneNumbers").forEach { number ->
                number.setObject("person", person)
            }
        }
    }
    // endregion PhoneNumber migrations

    // region Social Media migrations
    private inline fun DynamicRealm.createSocialMediaSchemas() {
        createFacebookSchema()
        createLinkedInSchema()
        createTwitterSchema()
        createWebsiteSchema()
    }

    private fun DynamicRealm.createFacebookSchema() {
        schema.create("FacebookAccount")
            .addField("id", String::class.java)
            .addPrimaryKey("id")
            .addRealmObjectField("person", schema["Person"]!!)
            .addField("username", String::class.java)
            .addChangeAwareFields()
    }

    private fun DynamicRealm.createWebsiteSchema() {
        schema.create("Website")
            .addField("id", String::class.java)
            .addPrimaryKey("id")
            .addRealmObjectField("person", schema["Person"]!!)
            .addField("url", String::class.java)
            .addChangeAwareFields()
    }

    private fun DynamicRealm.createLinkedInSchema() {
        schema.create("LinkedInAccount")
            .addField("id", String::class.java)
            .addPrimaryKey("id")
            .addRealmObjectField("person", schema["Person"]!!)
            .addField("publicUrl", String::class.java)
            .addChangeAwareFields()
    }

    private fun DynamicRealm.createTwitterSchema() {
        schema.create("TwitterAccount")
            .addField("id", String::class.java)
            .addPrimaryKey("id")
            .addRealmObjectField("person", schema["Person"]!!)
            .addField("screenName", String::class.java)
            .addChangeAwareFields()
    }
    // endregion Social Media migrations

    // region Tag migrations
    private inline fun DynamicRealm.addTagContactTagForField() =
        schema["Tag"]!!.addRealmListField("contactTagFor", schema["AccountList"]!!)
    // endregion Tag migrations

    // region Task migrations
    private inline fun DynamicRealm.convertTaskCompletedAtToDate() = schema["Task"]!!.apply {
        addField("newCompletedAt", Date::class.java)
        setRequiredIfNeeded("newCompletedAt", false)
        transform {
            it.getString("completedAt")?.let { date ->
                it.setDate("newCompletedAt", date.toInstanOrNull()?.toDate() ?: date.toLocalDateOrNull()?.toDate())
            }
        }
        removeField("completedAt")
        renameField("newCompletedAt", "completedAt")

        removeLastSyncFor("task")
    }

    private inline fun DynamicRealm.removeTaskContactIds() = schema["Task"]!!.apply {
        transform { task ->
            val isNew = task.getBoolean("isNew") || task.getString("changedFieldsStr")?.contains("contacts") == true
            task.getList("contactIds", String::class.java).forEach {
                val contact = where("Contact").equalTo("id", it).findFirst()
                if (contact != null) {
                    createObject("TaskContact", UUID.randomUUID().toString()).apply {
                        setBoolean("isNew", isNew)
                        setObject("contact", contact)
                        setObject("task", task)
                    }
                }
            }
        }
        removeField("contactIds")
        removeField("contactIdStr")

        removeLastSyncFor("task")
    }

    private inline fun DynamicRealm.addTaskAccountListRelationship() = schema["Task"]!!.apply {
        addRealmObjectField("accountList", schema["AccountList"]!!)
        transform {
            val accountListId = it.getString("accountListId") ?: appPrefs.accountListId
            val accountList = where("AccountList").equalTo("id", accountListId).findFirst()
                ?: if (accountListId != null) createObject("AccountList", accountListId) else null
            it.setObject("accountList", accountList)
        }
        removeField("accountListId")
    }

    private inline fun DynamicRealm.clearTaskHiddenSubjects() = schema["Task"]!!.apply {
        transform {
            if (!it.isNull("isSubjectHidden") && it.getBoolean("isSubjectHidden")) {
                it.setString("subject", null)
            }
        }
        removeLastSyncFor("task")
    }

    private fun DynamicRealm.addTaskUserRelationship() {
        schema["Task"]!!.addRealmObjectField("user", schema["User"]!!)
        removeLastSyncFor("task")
    }
    // endregion Task migrations

    // region TaskContact migrations
    private inline fun DynamicRealm.createTaskContactSchema() {
        schema.create("TaskContact")
            .addField("id", String::class.java)
            .addPrimaryKey("id")
            .addChangeAwareFields()
            .addRealmObjectField("contact", schema["Contact"]!!)
            .addRealmObjectField("task", schema["Task"]!!)
        removeLastSyncFor("task")
    }
    // endregion TaskContact migrations

    // region TaskAnalytics

    private inline fun DynamicRealm.addTaskAnalyticsAccountListId() = schema["TaskAnalytics"]!!.apply {
        addField("accountListId", String::class.java)
        setRequiredIfNeeded("accountListId", false)
    }
    // endregion TaskAnalytics

    // region Pledge migrations
    private fun RealmObjectSchema.convertPledgeDonationIdToListOfIds() {
        addRealmListField("donationIds", String::class.java)
        transform {
            it.getString("donationId")?.let { id ->
                it.setList("donationIds", RealmList(id))
            }
        }
        removeField("donationId")
    }

    private inline fun DynamicRealm.convertPledgeAmountToDouble() = schema["Pledge"]!!.apply {
        addField("newAmount", Double::class.java)
        setRequiredIfNeeded("newAmount", false)
        transform {
            it.getString("amount")
                ?.let { raw -> raw.toDoubleOrNull() ?: raw.localizedToDoubleOrNull() }
                ?.let { value -> it.setDouble("newAmount", value) }
        }
        removeField("amount")
        renameField("newAmount", "amount")

        removeLastSyncFor("pledge")
    }

    private inline fun DynamicRealm.convertPledgeAppealRelationship() = schema["Pledge"]!!.apply {
        addRealmObjectField("appeal", schema["Appeal"]!!)
        setRequiredIfNeeded("appeal", false)
        transform {
            val appeal = where("Appeal").equalTo("id", it.getString("appealId")).findFirst()
            it.setObject("appeal", appeal)
        }
        removeField("appealId")

        removeLastSyncFor("pledges")
    }

    private inline fun DynamicRealm.convertPledgeContactRelationship() = schema["Pledge"]!!.apply {
        addRealmObjectField("contact", schema["Contact"]!!)
        setRequiredIfNeeded("contact", false)
        transform {
            val contact = where("Contact").equalTo("id", it.getString("contactId")).findFirst()
            it.setObject("contact", contact)
        }
        removeField("contactId")

        removeLastSyncFor("pledges")
    }

    private inline fun DynamicRealm.addPledgeAccountListRelationship() = schema["Pledge"]!!.apply {
        addRealmObjectField("accountList", schema["AccountList"]!!)
        val accountList = where("AccountList").equalTo("id", appPrefs.accountListId).findFirst()
        transform {
            it.setObject("accountList", accountList)
        }
        removeLastSyncFor("pledges")
    }
    // endregion Pledge migrations

    // region DbUser migrations
    private fun DynamicRealm.removeUserUnusedFields() = schema["User"]!!.apply {
        removeField("masterPersonId")
        removeField("firstName")
        removeField("lastName")
        removeField("isCoachee")
        removeField("createdAt")
        removeField("updatedAt")
        removeField("updatedInDbAt")
    }

    private fun DynamicRealm.removeUserPreferenceFields() = schema["User"]!!.apply {
        removeField("defaultAccountList")
        removeField("setupStatus")
    }
    // endregion DbUser migrations

    // region User migrations
    private fun DynamicRealm.createUserModel() {
        schema.create("User")
            .addField("id", String::class.java)
            .addPrimaryKey("id")
            .addField("firstName", String::class.java)
            .addField("lastName", String::class.java)
    }

    private fun DynamicRealm.addUserAccountListRelationship() =
        schema["User"]!!.addRealmListField("accountLists", schema["AccountList"]!!)
    // region User migrations
}

private fun RealmObjectSchema.setRequiredIfNeeded(fieldName: String, required: Boolean) {
    if (isRequired(fieldName) != required) setRequired(fieldName, required)
}

private fun RealmObjectSchema.convertFieldFromStringToDouble(
    fieldName: String,
    tmpFieldName: String = "tmp_$fieldName"
) {
    addField(tmpFieldName, Double::class.java)
    setRequiredIfNeeded(tmpFieldName, isRequired(fieldName))
    transform {
        it.getString(fieldName)
            ?.let { raw -> raw.toDoubleOrNull() ?: raw.localizedToDoubleOrNull() }
            ?.let { value -> it.setDouble(tmpFieldName, value) }
    }
    removeField(fieldName)
    renameField(tmpFieldName, fieldName)
}

private fun DynamicRealmObject.getIntOrNull(fieldName: String) = if (isNull(fieldName)) null else getInt(fieldName)

private fun DynamicRealmObject.setInt(fieldName: String, value: Int?) =
    if (value == null) setNull(fieldName) else setInt(fieldName, value)
