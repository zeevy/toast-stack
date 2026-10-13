package com.siliconcircuits.toaststack.consumer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import com.siliconcircuits.toaststack.ToastPriority
import com.siliconcircuits.toaststack.ToastStack
import com.siliconcircuits.toaststack.ToastStackHost
import com.siliconcircuits.toaststack.buildToast
import com.siliconcircuits.toaststack.rememberToastStackState

/** Uses the main entry points, so the build checks they compile on the minimums. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state = rememberToastStackState()
            Button(onClick = {
                ToastStack.success("Saved")?.withAction("Undo") {}
                state.show("Local", priority = ToastPriority.High)
                buildToast { message = "Built" }
            }) {
                Text("Show")
            }
            ToastStackHost(state = state, tag = "local")
        }
    }
}
