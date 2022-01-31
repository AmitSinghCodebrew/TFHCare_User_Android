package com.consultantapp.ui.dashboard.doctor.symptom

import androidx.lifecycle.ViewModel
import com.consultantapp.data.apis.WebService
import com.consultantapp.data.models.requests.UpdateSymptom
import com.consultantapp.data.models.responses.CommonDataModel
import com.consultantapp.data.network.responseUtil.ApiResponse
import com.consultantapp.data.network.responseUtil.ApiUtils
import com.consultantapp.data.network.responseUtil.Resource
import com.consultantapp.di.SingleLiveEvent
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class PandemicViewModel @Inject constructor(private val webService: WebService) : ViewModel() {


    val symptom by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

    val updateSymptom by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }


    fun symptom(hashMap: HashMap<String, String>) {
        symptom.value = Resource.loading()

        webService.symptom(hashMap)
                .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                    override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                            response: Response<ApiResponse<CommonDataModel>>) {
                        if (response.isSuccessful) {
                            symptom.value = Resource.success(response.body()?.data)
                        } else {
                            symptom.value = Resource.error(
                                    ApiUtils.getError(response.code(),
                                            response.errorBody()?.string()))
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                        symptom.value = Resource.error(ApiUtils.failure(throwable))
                    }

                })
    }

    fun updateSymptom(updateSymptomModel: UpdateSymptom) {
        updateSymptom.value = Resource.loading()

        webService.updateSymptom(updateSymptomModel)
            .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                        response: Response<ApiResponse<CommonDataModel>>) {
                    if (response.isSuccessful) {
                        updateSymptom.value = Resource.success(response.body()?.data)
                    } else {
                        updateSymptom.value = Resource.error(
                            ApiUtils.getError(response.code(),
                                response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                    updateSymptom.value = Resource.error(ApiUtils.failure(throwable))
                }

            })
    }
}