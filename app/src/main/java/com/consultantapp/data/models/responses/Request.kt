package com.consultantapp.data.models.responses

import com.consultantapp.data.models.requests.DocImage
import com.consultantapp.data.models.requests.SecondOpinion
import java.io.Serializable

class Request : Serializable {

    var id: String? = null
    var booking_date: String? = null
    var from_user: UserData? = null
    var to_user: UserData? = null
    var time: String? = null
    var service_type: String? = null
    var schedule_type: String? = null
    var service_id: String? = null
    var price: String? = null
    var status: String? = null
    var rating: String? = null
    var created_at: String? = null
    var bookingDateUTC: String? = null
    var canReschedule = false
    var canCancel = false
    var canNotify = true

    var is_prescription: Boolean? = null
    var symptoms: List<Filter>? = null
    var symptom_details: String? = null
    var symptom_image: Feed? = null
    var symptom_images: List<DocImage>? = null

    var is_second_oponion: Boolean? = null
    var second_oponion: SecondOpinion? = null
}