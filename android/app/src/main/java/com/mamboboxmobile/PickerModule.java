package com.mamboboxmobile;

import android.app.Activity;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class PickerModule extends ReactContextBaseJavaModule implements ActivityEventListener {

	private static final int PICKER_REQUEST = 467081;
	private static final String E_ACTIVITY_DOES_NOT_EXIST = "E_ACTIVITY_DOES_NOT_EXIST";
	private static final String E_PICKER_CANCELLED = "E_PICKER_CANCELLED";
	private static final String E_FAILED_TO_SHOW_PICKER = "E_FAILED_TO_SHOW_PICKER";
	private static final String E_NO_DATA_FOUND = "E_NO_DATA_FOUND";

	private Promise mPickerPromise;

	public PickerModule(ReactApplicationContext reactContext) {
		super(reactContext);

		// Add the listener for `onActivityResult`
		reactContext.addActivityEventListener(this);
	}

	@Override
	public String getName() {
		return "PickerModule";
	}

	@ReactMethod
	public void pick(final Promise promise) {
		Activity currentActivity = getCurrentActivity();

		if (currentActivity == null) {
			promise.reject(E_ACTIVITY_DOES_NOT_EXIST, "Activity doesn't exist");
			return;
		}


		// Store the promise to resolve/reject when picker returns data
		mPickerPromise = promise;

		try {
			final Intent intent = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
			intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

			final Intent chooserIntent = Intent.createChooser(intent, "Pick a file");

			currentActivity.startActivityForResult(chooserIntent, PICKER_REQUEST);
		} catch (Exception e) {
			mPickerPromise.reject(E_FAILED_TO_SHOW_PICKER, e);
			mPickerPromise = null;
		}
	}

	private String getRealPathFromURI(Context context, Uri contentUri) {
		String[] proj = { MediaStore.Audio.Media.DATA };
		CursorLoader loader = new CursorLoader(context, contentUri, proj, null, null, null);
		Cursor cursor = loader.loadInBackground();
		int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
		cursor.moveToFirst();
		return cursor.getString(column_index);
	}

	// You can get the result here
	@Override
	public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
		if (requestCode == PICKER_REQUEST) {
			if (mPickerPromise != null) {
				if (resultCode == Activity.RESULT_CANCELED) {
					mPickerPromise.reject(E_PICKER_CANCELLED, "Picker was cancelled");
				} else if (resultCode == Activity.RESULT_OK) {
					Uri uri = intent.getData();

					if (uri == null) {
						mPickerPromise.reject(E_NO_DATA_FOUND, "No data found");
					} else {
						mPickerPromise.resolve(getRealPathFromURI(this.getReactApplicationContext(),uri));
					}
				}

				mPickerPromise = null;
			}
		}
	}

	@Override
	public void onNewIntent(Intent intent) {

	}
}
