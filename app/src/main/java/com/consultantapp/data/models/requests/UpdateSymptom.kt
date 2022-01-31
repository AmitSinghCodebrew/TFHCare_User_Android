package com.consultantapp.data.models.requests

import java.io.Serializable


class UpdateSymptom : Serializable {
    var request_id: String? = null
    var image: ArrayList<DocImage>? = null
    var images: ArrayList<DocImage>? = null
    var option_ids: String? = null
    var symptom_details: String? = null
}