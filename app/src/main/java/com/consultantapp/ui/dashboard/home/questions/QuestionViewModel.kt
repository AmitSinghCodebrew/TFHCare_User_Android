package com.consultantapp.ui.dashboard.home.questions

import androidx.lifecycle.ViewModel
import com.consultantapp.data.apis.WebService
import com.consultantapp.data.models.responses.CommonDataModel
import com.consultantapp.data.network.responseUtil.ApiResponse
import com.consultantapp.data.network.responseUtil.ApiUtils
import com.consultantapp.data.network.responseUtil.Resource
import com.consultantapp.di.SingleLiveEvent
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class QuestionViewModel @Inject constructor(private val webService: WebService) : ViewModel() {

    val getQuestions by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

    val getQuestionsDetails by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

    val askQuestion by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

    val supportPackages by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }


    fun getQuestions(hashMap: HashMap<String, String>) {
        getQuestions.value = Resource.loading()

        webService.getQuestions(hashMap)
                .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                    override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                            response: Response<ApiResponse<CommonDataModel>>) {
                        if (response.isSuccessful) {
                            getQuestions.value = Resource.success(response.body()?.data)
                        } else {
                            getQuestions.value = Resource.error(
                                    ApiUtils.getError(response.code(),
                                            response.errorBody()?.string()))
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                        getQuestions.value = Resource.error(ApiUtils.failure(throwable))
                    }

                })
    }

    fun getQuestionsDetails(hashMap: HashMap<String, String>) {
        getQuestionsDetails.value = Resource.loading()

        webService.getQuestionsDetails(hashMap)
                .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                    override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                            response: Response<ApiResponse<CommonDataModel>>) {
                        if (response.isSuccessful) {
                            getQuestionsDetails.value = Resource.success(response.body()?.data)
                        } else {
                            getQuestionsDetails.value = Resource.error(
                                    ApiUtils.getError(response.code(),
                                            response.errorBody()?.string()))
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                        getQuestionsDetails.value = Resource.error(ApiUtils.failure(throwable))
                    }

                })
    }

    fun askQuestion(hashMap: HashMap<String, Any>) {
        askQuestion.value = Resource.loading()

        webService.askQuestion(hashMap)
                .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                    override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                            response: Response<ApiResponse<CommonDataModel>>) {
                        if (response.isSuccessful) {
                            askQuestion.value = Resource.success(response.body()?.data)
                        } else {
                            askQuestion.value = Resource.error(
                                    ApiUtils.getError(response.code(),
                                            response.errorBody()?.string()))
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                        askQuestion.value = Resource.error(ApiUtils.failure(throwable))
                    }

                })
    }

    fun supportPackages(hashMap: HashMap<String, String>) {
        supportPackages.value = Resource.loading()

        webService.supportPackages(hashMap)
                .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                    override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                            response: Response<ApiResponse<CommonDataModel>>) {
                        if (response.isSuccessful) {
                            supportPackages.value = Resource.success(response.body()?.data)
                        } else {
                            supportPackages.value = Resource.error(
                                    ApiUtils.getError(response.code(),
                                            response.errorBody()?.string()))
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                        supportPackages.value = Resource.error(ApiUtils.failure(throwable))
                    }

                })
    }


}