package beproject.cbir;

import java.io.File;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import beproject.cbir.database.ObjectDatabase;

public class StartPage extends Activity implements OnClickListener {

	boolean showToast = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.startlayout);

		try {
			File f = new File(Environment.getExternalStorageDirectory()
					+ "/TagIt/");
			f.mkdirs();
			/*
			 * f = new File(Environment.getExternalStorageDirectory() +
			 * "/TagIt/.nomedia"); f.createNewFile();
			 */
		} catch (Exception e) {
			e.printStackTrace();
		}

		Button bTrain, bSBK, bSBI, bTagAuto;

		bTrain = (Button) findViewById(R.id.bTrain);
		bSBK = (Button) findViewById(R.id.bSBK);
		bSBI = (Button) findViewById(R.id.bSBI);
		bTagAuto = (Button) findViewById(R.id.bTagAuto);

		bTrain.setOnClickListener(this);
		bSBI.setOnClickListener(this);
		bSBK.setOnClickListener(this);
		bTagAuto.setOnClickListener(this);

		setTypeFace(bTrain, "cnlbold.ttf");
		setTypeFace(bSBI, "cnlbold.ttf");
		setTypeFace(bSBK, "cnlbold.ttf");
		setTypeFace(bTagAuto, "cnlbold.ttf");
		setTypeFace(findViewById(R.id.tvTitle), "cnl.ttf");

		

	}

	/*
	 * Sets the font on all TextViews in the ViewGroup. Searches recursively for
	 * all inner ViewGroups as well. Just add a check for any other views you
	 * want to set as well (EditText, etc.)
	 */
	public void setTypeFace(ViewGroup group, String tfs) {
		Typeface font = Typeface.createFromAsset(getAssets(), tfs);
		int count = group.getChildCount();
		View v;
		for (int i = 0; i < count; i++) {
			v = group.getChildAt(i);
			if (v instanceof TextView || v instanceof EditText
					|| v instanceof Button) {
				((TextView) v).setTypeface(font);
			} else if (v instanceof ViewGroup)
				setTypeFace((ViewGroup) v, tfs);
		}
	}

	void setTypeFace(View v, String tfs) {
		Typeface tf = Typeface.createFromAsset(getAssets(), tfs);
		if (v instanceof Button || v instanceof TextView) {
			((TextView) v).setTypeface(tf);
		}
	}

	@Override
	protected void onResume() {
		ObjectDatabase od = new ObjectDatabase(StartPage.this);
		od.open();
		od.deleteUnworthyTags();
		od.close();

		if (new File(Environment.getExternalStorageDirectory()
				+ "/TagIt/.temp/").exists()) {
			File[] files = new File(Environment.getExternalStorageDirectory()
					+ "/TagIt/.temp/").listFiles();
			for (int i = 0; i < files.length; ++i) {
				File file = files[i];
				if (file.isDirectory()) {

				} else {
					file.delete();

				}
			}
		}

		new RefreshTags().execute();
		super.onResume();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.bTrain:
			MakeDialog(0);
			break;
		case R.id.bTagAuto:
			MakeDialog(1);
			break;
		case R.id.bSBK:
			startActivity(new Intent(StartPage.this,
					KeywordSearchActivity.class));
			break;
		case R.id.bSBI:
			MakeDialog(2);
			break;

		}

	}

	private static final int ACTION_REQUEST_CAMERA = 90;
	private static final int ACTION_REQUEST_GALLERY = 99;
	File file;

	/**
	 * Start the activity to pick an image from the user gallery
	 */
	private void pickFromGallery(int reqCode) {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");

		Intent chooser = Intent.createChooser(intent, "Choose a Picture");
		startActivityForResult(chooser, ACTION_REQUEST_GALLERY + reqCode);
	}

	void MakeDialog(final int reqCode) {
		AlertDialog.Builder builder = new AlertDialog.Builder(StartPage.this);
		builder.setTitle("Select Method");
		final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
				StartPage.this, android.R.layout.simple_list_item_1);
		arrayAdapter.add("Take Photo");
		arrayAdapter.add("Choose from Gallery");
		builder.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});

		builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == 0) {
					String path = Environment.getExternalStorageDirectory()
							+ "/TagIt/Database/Captured/";

					file = new File(path);
					file.mkdirs();
					file = new File(path + "/TI_" + System.currentTimeMillis()
							+ ".jpg");
					Uri outputFileUri = Uri.fromFile(file);
					Intent intent = new Intent(
							android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
					intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
					startActivityForResult(intent, ACTION_REQUEST_CAMERA
							+ reqCode);
					// startActivityForResult(
					// new Intent(
					// android.provider.MediaStore.ACTION_IMAGE_CAPTURE),
					// ACTION_REQUEST_CAMERA);
				} else {
					pickFromGallery(reqCode);
				}
			}
		});
		builder.show();

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		Log.d("StartingPoint", "Result Recieved");

		if (resultCode == RESULT_OK) {

			Log.d("StartingPoint", "Got the Image");
			String picturePath;
			Uri selectedImage;
			switch (requestCode) {

			case ACTION_REQUEST_CAMERA:
				picturePath = file.getAbsolutePath();
				startActivity(new Intent(getBaseContext(), TrainActivity.class)
						.putExtra("ImgPath", picturePath));
				break;
			case (ACTION_REQUEST_CAMERA + 1):
				picturePath = file.getAbsolutePath();
				startActivity(new Intent(getBaseContext(), TagActivity.class)
						.putExtra("ImgPath", picturePath));
				break;
			case (ACTION_REQUEST_CAMERA + 2):
				picturePath = file.getAbsolutePath();
				startActivity(new Intent(getBaseContext(),
						ImageSearchActivity.class).putExtra("ImgPath",
						picturePath));
				break;
			case ACTION_REQUEST_GALLERY:
				selectedImage = data.getData();
				picturePath = getPath(selectedImage);
				startActivity(new Intent(getBaseContext(), TrainActivity.class)
						.putExtra("ImgPath", picturePath));
				break;
			case ACTION_REQUEST_GALLERY + 1:
				selectedImage = data.getData();
				picturePath = getPath(selectedImage);
				startActivity(new Intent(getBaseContext(), TagActivity.class)
						.putExtra("ImgPath", picturePath));
				break;
			case ACTION_REQUEST_GALLERY + 2:
				selectedImage = data.getData();
				picturePath = getPath(selectedImage);
				startActivity(new Intent(getBaseContext(),
						ImageSearchActivity.class).putExtra("ImgPath",
						picturePath));
				break;

			}
		}
	}

	/**
	 * helper to retrieve the path of an image URI
	 */
	public String getPath(Uri uri) {
		if (uri == null) {
			return null;
		}
		String[] filePathColumn = { MediaStore.Images.Media.DATA };

		Cursor cursor = getContentResolver().query(uri, filePathColumn, null,
				null, null);
		if (cursor != null) {
			cursor.moveToFirst();

			int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
			String picturePath = cursor.getString(columnIndex);
			cursor.close();

			if (picturePath == null
					&& Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				return getKitkatPath(uri);
			}

			return picturePath;
		}
		return uri.getPath();
	}

	@SuppressLint("NewApi")
	private String getKitkatPath(Uri uri) {
		final String docId = DocumentsContract.getDocumentId(uri);
		final String[] split = docId.split(":");

		Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

		final String selection = "_id=?";
		final String[] selectionArgs = new String[] { split[1] };

		return getDataColumn(getBaseContext(), contentUri, selection,
				selectionArgs);
	}

	public static String getDataColumn(Context context, Uri uri,
			String selection, String[] selectionArgs) {

		Cursor cursor = null;
		final String column = "_data";
		final String[] projection = { column };

		try {
			cursor = context.getContentResolver().query(uri, projection,
					selection, selectionArgs, null);
			if (cursor != null && cursor.moveToFirst()) {
				final int index = cursor.getColumnIndexOrThrow(column);
				return cursor.getString(index);
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main_menu, menu);

		return true;
	}

	ArrayList<File> imgFile = new ArrayList<File>();
	final File dir = new File(Environment.getExternalStorageDirectory()
			+ "/TagIt/.Tags/");

	public void traverse(File dir) {
		if (dir.exists()) {
			File[] files = dir.listFiles();
			for (int i = 0; i < files.length; ++i) {
				File file = files[i];
				if (file.isDirectory()) {
					traverse(file);
				} else {
					imgFile.add(file);

				}
			}
		}
	}

	public class RefreshTags extends AsyncTask<Void, Void, Void> {

		ObjectDatabase db;

		@Override
		protected void onPostExecute(Void result) {
			db.close();
			if (showToast)
				Toast.makeText(getBaseContext(), "Refresh Completed.",
						Toast.LENGTH_LONG).show();
			showToast = false;
			super.onPostExecute(result);
		}

		@Override
		protected void onPreExecute() {
			traverse(dir);
			// Toast.makeText(getBaseContext(), "Refresh Started!!",
			// Toast.LENGTH_LONG).show();
			db = new ObjectDatabase(StartPage.this);

			db.open();
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... params) {
			for (File img : imgFile)
				db.CreateOrUpdateEntry(img.getPath(), 1, 1);

			ArrayList<String> paths = db.GetPaths();
			Log.d("Paths", paths.toString());
			if (paths != null) {
				for (int i = 0; i < paths.size(); i++)

					if (!(new File(paths.get(i))).exists()) {
						Log.d("TagIt!", paths.get(i) + " doesn't exists!!");
						db.deleteEntry(paths.get(i));
					} else
						Log.d("TagIt!", paths.get(i) + " exists!!");
			}
			return null;
		}

	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (item.getItemId() == R.id.refresh) {
			showToast = true;
			new RefreshTags().execute();

		}
		return super.onMenuItemSelected(featureId, item);
	}

}
