package com.consultantapp.data.models.responses

import java.io.Serializable

class Feed : Serializable {
    var id: String? = null
    var title: String? = null
    var amount: String? = null
    var image: String? = null
    var description: String? = null
    var like: Any? = null
    var user_id: Int? = null
    var created_at: String? = null
    var user_data: UserData? = null

    var favorite: String? = null
    var views: String? = null
    var is_favorite: Boolean? = null

    var comment: String? = null
    var user: UserData? = null

    /*Ask Question*/
    var answers: List<Feed>? = null
    var answer: String? = null

    /*Support Package*/
    var image_icon: String? = null
    var color_code: String? = null
    var price: String? = null

    /*Symptom Image*/
    var type: String? = null
}