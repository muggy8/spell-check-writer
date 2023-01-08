package com.muggy8.spell_check_writer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermssionController (val context: AppCompatActivity) {
    private val missingPermissions = mutableListOf<String>()
    private val RECORD_REQUEST_CODE = 1337 // i have no idea what this is for but it seems like it should be constant

    fun hasFileAccessPermission():Boolean{
        var hasPermission = true
        missingPermissions.clear()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val hasManageExternalStoragePermission = Environment.isExternalStorageManager()
            hasPermission = hasPermission && hasManageExternalStoragePermission
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            val hasReadPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasReadPermission){
                missingPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            hasPermission = hasPermission && hasReadPermission
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val hasWritePermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasWritePermission){
                missingPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }

            hasPermission = hasPermission && hasWritePermission
        }

        return hasPermission
    }

    fun requestFileAccessPermission(){
        hasFileAccessPermission()
        if (!missingPermissions.isEmpty()){
            ActivityCompat.requestPermissions(
                context,
                missingPermissions.toTypedArray(),
                RECORD_REQUEST_CODE,
            )
        }
        else {
            try {
                val intentToGetManageStoragePermission = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + BuildConfig.APPLICATION_ID)
                )
                context.startActivity(intentToGetManageStoragePermission)
            }
            catch(e: Exception){
                val intentToGetManageStoragePermission =
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                context.startActivity(intentToGetManageStoragePermission)
            }
        }
    }


    fun requestPermissionLabelTextRes():Int{
        if (hasFileAccessPermission()){
            throw Error("Permission already granted")
        }
        if (missingPermissions.isEmpty()){
            return R.string.grant_manage_storage_permission
        }
        return R.string.grant_storage_permission
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

    }
    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

    }
}