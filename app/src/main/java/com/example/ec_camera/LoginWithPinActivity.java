package com.example.ec_camera;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class LoginWithPinActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_with_pin);
        // Create the Keyboard
        EditText emp_No = (EditText) findViewById(R.id.edt_empno);
        MyKeyboard keyboard = (MyKeyboard) findViewById(R.id.keyboardview);

        // prevent system keyboard from appearing when EditText is tapped
        emp_No.setRawInputType(InputType.TYPE_CLASS_TEXT);
        emp_No.setTextIsSelectable(true);

        // pass the InputConnection from the EditText to the keyboard
        InputConnection ic = emp_No.onCreateInputConnection(new EditorInfo());
        keyboard.setInputConnection(ic);
    }
}