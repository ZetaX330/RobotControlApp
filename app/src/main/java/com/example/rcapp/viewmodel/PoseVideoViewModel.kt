package com.example.rcapp.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PoseVideoViewModel : ViewModel() {
    private val _videoUri = MutableLiveData<Uri>()
    val videoUri: LiveData<Uri> get() = _videoUri
    fun setVideoUri(uri: Uri) {
        _videoUri.value = uri
    }


}