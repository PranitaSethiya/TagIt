package beproject.cbir;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import android.animation.Animator;
import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import beproject.cbir.database.ObjectDatabase;
import beproject.cbir.database.TagsDatabase;

import com.edmodo.cropper.CropImageView;

public class TrainActivity extends Activity {

	ViewGroup container = null;
	Animator defaultAppearingAnim, defaultDisappearingAnim;
	Animator defaultChangingAppearingAnim, defaultChangingDisappearingAnim;
	Bitmap bitmap;
	CropImageView iv;
	File files = new File(Environment.getExternalStorageDirectory() + "/TagIt/"
			+ ".temp/TS_temp" + ".jpg");
	Bitmap croppedbitmap;
	ArrayList<String> Tags = new ArrayList<String>();
	ArrayList<String> TagsPath = new ArrayList<String>();
	String imagePath;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			getActionBar().setBackgroundDrawable(
					getResources().getDrawable(R.drawable.actionbar_back));

		// this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.train);

		if (!getIntent().hasExtra("ImgPath")) {
			Toast.makeText(getBaseContext(), "Error Occurred!!",
					Toast.LENGTH_LONG).show();
			finish();
		}

		imagePath = getIntent().getStringExtra("ImgPath");

		bitmap = BitmapFactory
				.decodeFile(getIntent().getStringExtra("ImgPath"));
		PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit()
				.putString("temp", getIntent().getStringExtra("ImgPath"))
				.commit();
		// et = (EditText) findViewById(R.id.etaddNewButton);
		// setTypeFace(et, "cnl.ttf");
		setTypeFace(((TextView) findViewById(R.id.tvTags)), "cnl.ttf");

		float newWidth, newHeight;

		if (bitmap.getHeight() >= bitmap.getWidth()) {
			newHeight = (float) (getResources().getDisplayMetrics().heightPixels * 0.6);
			newWidth = (float) ((float) (newHeight * bitmap.getWidth()) / (float) bitmap
					.getHeight());
			bitmap = Bitmap.createScaledBitmap(bitmap, (int) newWidth,
					(int) newHeight, true);
		} else if (bitmap.getWidth() < (float) (getResources()
				.getDisplayMetrics().widthPixels * 0.9)
				&& bitmap.getHeight() < (float) (getResources()
						.getDisplayMetrics().heightPixels * 0.6)) {
			newWidth = (float) (getResources().getDisplayMetrics().widthPixels * 0.9);
			newHeight = (float) ((float) (newWidth * bitmap.getHeight()) / (float) bitmap
					.getWidth());
			bitmap = Bitmap.createScaledBitmap(bitmap, (int) newWidth,
					(int) newHeight, true);
		}

		iv = (CropImageView) findViewById(R.id.ivTrainImage);
		iv.setAspectRatio(DEFAULT_ASPECT_RATIO_VALUES,
				DEFAULT_ASPECT_RATIO_VALUES);

		/*
		 * LayoutParams params = iv.getLayoutParams();
		 * 
		 * params.height = (int) bitmap.getHeight(); params.width =
		 * (int)bitmap.getWidth();
		 * 
		 * iv.setLayoutParams(params);
		 */
		iv.setImageBitmap(bitmap);
		Bitmap bmps = Bitmap.createBitmap(bitmap.getWidth(),
				bitmap.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(bmps);
		canvas.drawColor(Color.BLACK);
		((ImageView) findViewById(R.id.ivback)).setImageBitmap(bmps);

		/*
		 * params = ((ImageView)findViewById(R.id.ivback)).getLayoutParams();
		 * 
		 * params.height = (int) bitmap.getHeight(); params.width =
		 * (int)bitmap.getWidth();
		 * 
		 * ((ImageView)findViewById(R.id.ivback)).setLayoutParams(params);
		 */

		container = new FixedGridLayout(this);
		container.setClipChildren(false);
		((FixedGridLayout) container).setCellHeight(ReturnHeight(50,
				getBaseContext()));
		((FixedGridLayout) container).setCellWidth(ReturnHeight(93,
				getBaseContext()));
		final LayoutTransition transitioner = new LayoutTransition();
		container.setLayoutTransition(transitioner);
		defaultAppearingAnim = transitioner
				.getAnimator(LayoutTransition.APPEARING);
		defaultDisappearingAnim = transitioner
				.getAnimator(LayoutTransition.DISAPPEARING);
		defaultChangingAppearingAnim = transitioner
				.getAnimator(LayoutTransition.CHANGE_APPEARING);
		defaultChangingDisappearingAnim = transitioner
				.getAnimator(LayoutTransition.CHANGE_DISAPPEARING);
		ViewGroup parent = (ViewGroup) findViewById(R.id.llTagsGrid);
		parent.addView(container);
		parent.setClipChildren(false);

		Button addButton = (Button) findViewById(R.id.baddNewButton);
		setTypeFace(addButton, "cnlbold.ttf");
		addButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				if (files.exists()) {
					files.delete();
				}

				croppedbitmap = Bitmap.createBitmap(iv.getCroppedImage());

				try {
					FileOutputStream outs = null;

					File dirs = new File(Environment
							.getExternalStorageDirectory() + "/TagIt/.temp/");
					if (!dirs.exists())
						dirs.mkdirs();

					outs = new FileOutputStream(files);
					croppedbitmap.compress(Bitmap.CompressFormat.JPEG, 100,
							outs);
				} catch (Exception ex) {
				}

				startActivityForResult(new Intent(TrainActivity.this,
						DialogActivity.class), 999);

			}

		});

		if (getIntent().hasExtra("tagBundle")) {
			Bundle b = getIntent().getBundleExtra("tagBundle");
			Tags = b.getStringArrayList("tags");
			TagsPath = b.getStringArrayList("tagsPath");
			for (int i = 0; i < Tags.size(); i++) {
				Button newButton = new Button(TrainActivity.this);
				newButton.setText(Tags.get(i));
				newButton.setTag(TagsPath.get(i));
				newButton.setMinWidth(ReturnHeight(93, getBaseContext()));
				newButton.setMinHeight(ReturnHeight(50, getBaseContext()));
				newButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
				newButton.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {

						Tags.remove(((Button) v).getText());
						try {
							ObjectDatabase db = new ObjectDatabase(
									TrainActivity.this);
							db.open();
							db.updateEntry(v.getTag().toString(), 0, 1);
							db.close();
							TagsPath.remove(v.getTag().toString());
						} catch (Exception ex) {
							Log.d("Minus Update", ex.toString());
						}
						container.removeView(v);

					}
				});
				container.addView(newButton);
			}
		}

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK && requestCode == 999) {
			Button newButton = new Button(TrainActivity.this);
			newButton.setText(data.getStringExtra("tag"));
			newButton.setTag(data.getStringExtra("path"));
			newButton.setMinWidth(ReturnHeight(93, getBaseContext()));
			newButton.setMinHeight(ReturnHeight(50, getBaseContext()));
			Tags.add(data.getStringExtra("tag"));
			TagsPath.add(data.getStringExtra("path"));
			newButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
			newButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {

					File mfile = new File(String.valueOf(v.getTag()));
					if (mfile.exists())
						mfile.delete();
					container.removeView(v);

				}
			});
			container.addView(newButton);
			reload();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	public void save() {

		TagsDatabase db = new TagsDatabase(TrainActivity.this);
		db.open();
		String tag = " ";
		for (int i = 0; i < Tags.size(); i++)
			tag = tag + Tags.get(i) + " , ";
		db.CreateOrUpdateEntry(imagePath, tag);
		db.close();

		ObjectDatabase odb = new ObjectDatabase(TrainActivity.this);

		odb.open();

		for (int i = 0; i < TagsPath.size(); i++) {
			odb.updateEntry(TagsPath.get(i), 1, 1);
		}

		odb.close();

		Toast.makeText(getBaseContext(), "Image saved to database!!",
				Toast.LENGTH_LONG).show();
	}

	@SuppressLint("NewApi")
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem save = menu.add("Save");
		save.setIcon(android.R.drawable.ic_menu_save);
		save.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		save.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				save();
				return true;
			}
		});
		// MenuItem help = menu.add("Help");
		// help.setIcon(android.R.drawable.ic_menu_help);
		// help.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		// MenuItem rotate = menu.add("Rotate");
		// rotate
		return super.onCreateOptionsMenu(menu);
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
		if (v instanceof Button || v instanceof TextView
				|| v instanceof EditText) {
			((TextView) v).setTypeface(tf);
		}
	}

	/**
	 * Convert dp to px
	 * 
	 * @author Sachin
	 * @param i
	 * @param mContext
	 * @return
	 */

	public static int ReturnHeight(float i, Context mContext) {

		DisplayMetrics displayMetrics = mContext.getResources()
				.getDisplayMetrics();
		return (int) ((i * displayMetrics.density) + 0.5);

	}

	private static final int DEFAULT_ASPECT_RATIO_VALUES = 10;
	private static final String ASPECT_RATIO_X = "ASPECT_RATIO_X";
	private static final String ASPECT_RATIO_Y = "ASPECT_RATIO_Y";

	// Instance variables

	private int mAspectRatioX = DEFAULT_ASPECT_RATIO_VALUES;
	private int mAspectRatioY = DEFAULT_ASPECT_RATIO_VALUES;

	@Override
	protected void onSaveInstanceState(Bundle bundle) {
		super.onSaveInstanceState(bundle);
		bundle.putInt(ASPECT_RATIO_X, mAspectRatioX);
		bundle.putInt(ASPECT_RATIO_Y, mAspectRatioY);
	}

	// Restores the state upon rotating the screen/restarting the activity
	@Override
	protected void onRestoreInstanceState(Bundle bundle) {
		super.onRestoreInstanceState(bundle);
		mAspectRatioX = bundle.getInt(ASPECT_RATIO_X);
		mAspectRatioY = bundle.getInt(ASPECT_RATIO_Y);
	}

	@Override
	protected void onResume() {
		reload();

		super.onResume();
	}

	void reload() {
		try {
			bitmap = null;
			bitmap = BitmapFactory.decodeFile(PreferenceManager
					.getDefaultSharedPreferences(getBaseContext()).getString(
							"temp", ""));
			float newWidth = 0, newHeight = 0;

			if (bitmap.getHeight() >= bitmap.getWidth()) {
				newHeight = (float) (getResources().getDisplayMetrics().heightPixels * 0.6);
				newWidth = (float) ((float) (newHeight * bitmap.getWidth()) / (float) bitmap
						.getHeight());
				bitmap = Bitmap.createScaledBitmap(bitmap, (int) newWidth,
						(int) newHeight, true);
			} else if (bitmap.getWidth() < (float) (getResources()
					.getDisplayMetrics().widthPixels * 0.9)
					&& bitmap.getHeight() < (float) (getResources()
							.getDisplayMetrics().heightPixels * 0.6)) {
				newWidth = (float) (getResources().getDisplayMetrics().widthPixels * 0.9);
				newHeight = (float) ((float) (newWidth * bitmap.getHeight()) / (float) bitmap
						.getWidth());
				bitmap = Bitmap.createScaledBitmap(bitmap, (int) newWidth,
						(int) newHeight, true);
			}
			iv.setImageBitmap(bitmap);
			Bitmap bmps = Bitmap.createBitmap(bitmap.getWidth(),
					bitmap.getHeight(), Config.ARGB_8888);
			Canvas canvas = new Canvas(bmps);
			canvas.drawColor(Color.BLACK);
			((ImageView) findViewById(R.id.ivback)).setImageBitmap(bmps);

			/*
			 * LayoutParams params = iv.getLayoutParams();
			 * 
			 * params.height = (int) bitmap.getHeight(); params.width =
			 * (int)bitmap.getWidth();
			 * 
			 * iv.setLayoutParams(params);
			 * 
			 * params =
			 * ((ImageView)findViewById(R.id.ivback)).getLayoutParams();
			 * 
			 * params.height = (int) bitmap.getHeight(); params.width =
			 * (int)bitmap.getWidth();
			 * 
			 * ((ImageView)findViewById(R.id.ivback)).setLayoutParams(params);
			 */
			// Toast.makeText(getBaseContext(), "width: " + bitmap.getWidth() +
			// " height: " + bitmap.getHeight() + " newWidth: " + newWidth +
			// " newHeight: " + newHeight, Toast.LENGTH_LONG).show();

		} catch (Exception ex) {
		}
	}

}
