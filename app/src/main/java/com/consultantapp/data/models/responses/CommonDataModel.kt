package com.consultantapp.data.models.responses

import com.consultantapp.data.models.requests.DocImage
import com.consultantapp.data.models.responses.chat.ChatList
import com.consultantapp.data.models.responses.chat.ChatMessage


class CommonDataModel {
    var requests: List<Request>? = null
    var doctors: List<Doctor>? = null
    var review_list: List<Review>? = null
    var dcotor_detail: UserData? = null
    var messages: List<ChatMessage>? = null
    var lists: List<ChatList>? = null
    var balance: String? = null
    var payments: List<Wallet>? = null
    var notifications: List<Notification>? = null
    var cards: List<Wallet>? = null
    var classes_category: List<Categories>? = null
    var classes: List<ClassData>? = null
    var filters: List<Filter>? = null
    var services: List<Service>? = null
    var interval: List<Interval>? = null
    var banners: List<Banner>? = null
    var coupons: List<Banner>? = null

    var image_name: String? = null
    var request_status: String? = null
    var currentTimer: Long? = null

    var isOnline: Boolean? = null
    var CALLING_TYPE: String? = null
    var order_id: String? = null

    /*Create Request*/
    var amountNotSufficient: Boolean? = null

    /*Twili*/
    var twilioToken: String? = null

    /*Confirm Booking*/
    var total: String? = null
    var discount: String? = null
    var grand_total: String? = null
    var book_slot_time: String? = null
    var book_slot_date: String? = null
    var is_paid :Boolean ?=null
    var left_minute :Long ?=null

    /*Add money stripe authentication*/
    var requires_source_action: Boolean? = null
    var url: String? = null
    var transaction_id: String? = null


    /*Pages*/
    var pages: List<Page>? = null


    /*Country*/
    var type: String? = null
    var country: List<CountryCity>? = null
    var city: List<CountryCity>? = null
    var state: List<CountryCity>? = null

    /*Package*/
    var packages: List<Packages>? = null
    var detail: Packages? = null

    /*Feeds*/
    var feeds: List<Feed>? = null
    var feed: Feed? = null

    /*Home*/
    var questions: List<Feed>? = null
    var question: Feed? = null

    var symptoms: List<Filter>? = null

    var request: Request? = null

    /*Request*/
    var request_detail: Request? = null

    var support_packages: List<Feed>? = null

    var image: DocImage? = null

}