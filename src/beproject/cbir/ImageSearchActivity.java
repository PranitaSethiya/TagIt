package beproject.cbir;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;
import org.opencv.imgproc.Imgproc;

import android.animation.Animator;
import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import beproject.cbir.database.ObjectDatabase;
import beproject.cbir.database.TagsDatabase;
import beproject.cbir.smoothprogressbar.SmoothProgressBar;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;

public class ImageSearchActivity extends Activity {

	Bitmap bitmap;
	ViewGroup container = null;
	boolean once = true;
	String imagePath;
	public final String TAG = "BEProject";
	Animator defaultAppearingAnim, defaultDisappearingAnim;
	Animator defaultChangingAppearingAnim, defaultChangingDisappearingAnim;
	final File dir = new File(Environment.getExternalStorageDirectory()
			+ "/TagIt/.Tags/");

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");

				if (once) {
					TagsDatabase tb = new TagsDatabase(ImageSearchActivity.this);
					tb.open();
					if (tb.CheckIsDataAlreadyInDBorNot(imagePath)) {
						Tags = tb.GetTags(imagePath);

						new Handler().post(r);

					} else
						new LetsTagThem().execute(bitmap);
					tb.close();

				}
				once = false;
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	Runnable r = new Runnable() {

		@Override
		public void run() {
			for (int i = 0; i < Tags.size(); i++) {
				Button newButton = new Button(ImageSearchActivity.this);
				newButton.setText(Tags.get(i));

				// newButton.setTag(values[0]);
				newButton.setMinWidth(ReturnHeight(93, getBaseContext()));
				newButton.setMinHeight(ReturnHeight(50, getBaseContext()));
				newButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
				newButton.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {

						container.removeView(v);
						Tags.remove(((Button) v).getText());

						doWork();

					}
				});

				container.addView(newButton);
			}

			post();
		}
	};

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			getActionBar().setBackgroundDrawable(
					getResources().getDrawable(R.drawable.actionbar_back));

		}

		// this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.setContentView(R.layout.imagesearch);

		if (!getIntent().hasExtra("ImgPath")) {
			Toast.makeText(getBaseContext(), "Error Occurred!!",
					Toast.LENGTH_LONG).show();
			finish();
		}

		bitmap = BitmapFactory
				.decodeFile(getIntent().getStringExtra("ImgPath"));

		imagePath = getIntent().getStringExtra("ImgPath");

		ImageView iv = (ImageView) findViewById(R.id.ivTagImage);

		iv.setImageBitmap(bitmap);

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
		ViewGroup parent = (ViewGroup) findViewById(R.id.llforTagsGrid);
		parent.addView(container);
		parent.setClipChildren(false);

	}

	void post() {
		((SmoothProgressBar) findViewById(R.id.smoothProgressBar1))
				.setVisibility(View.GONE);
		((ProgressBar) findViewById(R.id.progressBar1))
				.setVisibility(View.GONE);
		// Toast.makeText(getBaseContext(), "Done!!", Toast.LENGTH_LONG)
		// .show();

		((TextView) findViewById(R.id.tvforTags)).setText("Tags:");
		save.setVisible(true);

		doWork();
	}

	ArrayList<File> imgFile = new ArrayList<File>();

	public void traverse(File dir) {
		if (dir.exists()) {
			File[] files = dir.listFiles();
			for (int i = 0; i < files.length; ++i) {
				File file = files[i];
				if (file.isDirectory()) {
					traverse(file);
				} else {
					post = file.getName();
					imgFile.add(file);

				}
			}
		}
	}

	MenuItem save;

	@SuppressLint("NewApi")
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		save = menu.add("Save");

		save.setIcon(android.R.drawable.ic_menu_save);
		save.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		save.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				if (Tags.size() == 0) {
					Toast.makeText(getBaseContext(),
							"No tags exists!!\nPlease add atleast one tag!!",
							Toast.LENGTH_LONG).show();
					return true;
				}
				save();
				return true;
			}
		});

		// MenuItem help = menu.add("Help");
		// help.setIcon(android.R.drawable.ic_menu_help);
		// help.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		if (Tags.size() > 0) {
			save.setVisible(true);

		} else {
			save.setVisible(false);

		}
		return super.onCreateOptionsMenu(menu);
	}

	public void save() {

		TagsDatabase db = new TagsDatabase(ImageSearchActivity.this);
		db.open();
		String tag = " ";
		for (int i = 0; i < Tags.size(); i++)
			tag = tag + Tags.get(i) + " , ";
		db.CreateOrUpdateEntry(imagePath, tag);
		db.close();

		ObjectDatabase odb = new ObjectDatabase(ImageSearchActivity.this);

		odb.open();

		for (int i = 0; i < TagsPath.size(); i++) {
			odb.updateEntry(TagsPath.get(i), 1, 1);
		}

		odb.close();

		Toast.makeText(getBaseContext(), "Image saved to database!!",
				Toast.LENGTH_LONG).show();
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

	public class LetsTagThem extends AsyncTask<Bitmap, String, Void> {

		Bitmap bmpimg1;
		Bitmap bmpimg2;
		Mat img1;
		Mat img2;
		Mat img3;
		ArrayList<String> selFol = new ArrayList<String>();

		@Override
		protected Void doInBackground(Bitmap... params) {

			traverse(dir);
			for (File img : imgFile) {
				if (selFol.contains(img.getParent())) {
					Log.d("skipping", img.getParent() + "/" + img.getName());
					continue;
				}
				Log.d("working", img.getParent() + "/" + img.getName());
				int newheight = Math
						.round((((float) bitmap.getHeight() / (float) bitmap
								.getWidth()) * 500));
				Bitmap bmpimg1 = BitmapFactory.decodeFile(img.getPath());
				Bitmap bmpimg2 = Bitmap.createScaledBitmap(bitmap, 500,
						newheight, true);
				Mat img1 = new Mat();
				Utils.bitmapToMat(bmpimg1, img1);
				Mat img2 = new Mat();
				Utils.bitmapToMat(bmpimg2, img2);

				Mat img3 = new Mat();
				Utils.bitmapToMat(bmpimg2, img3);

				Imgproc.cvtColor(img1, img1, Imgproc.COLOR_RGBA2GRAY);
				Imgproc.cvtColor(img2, img2, Imgproc.COLOR_RGBA2GRAY);
				// img1.convertTo(img1, CvType.CV_32F);
				// img2.convertTo(img2, CvType.CV_32F);

				// -- Step 1: Detect the keypoints using SURF Detector
				// int minHessian = 400;
				MatOfKeyPoint keypoints_object = new MatOfKeyPoint(), keypoints_scene = new MatOfKeyPoint();

				FeatureDetector surf = FeatureDetector
						.create(FeatureDetector.PYRAMID_FAST);

				surf.detect(img1, keypoints_object);
				surf.detect(img2, keypoints_scene);

				// -- Step 2: Calculate descriptors (feature vectors)

				// FeatureDetector fastFeatureDetector = FeatureDetector
				// .create(FeatureDetector.FAST);
				DescriptorExtractor surfDescriptorExtractor = DescriptorExtractor
						.create(DescriptorExtractor.BRISK);// best BRISK

				Mat descriptors_object = new Mat(), descriptors_scene = new Mat();

				surfDescriptorExtractor.compute(img1, keypoints_object,
						descriptors_object);
				surfDescriptorExtractor.compute(img2, keypoints_scene,
						descriptors_scene);

				// -- Step 3: Matching descriptor vectors using FLANN
				// matcher
				MatOfDMatch matches = new MatOfDMatch();// ArrayList<MatOfDMatch>();
				// matches.add(new MatOfDMatch());
				DescriptorMatcher flannDescriptorMatcher = DescriptorMatcher
						.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
				flannDescriptorMatcher.match(descriptors_object,
						descriptors_scene, matches);

				List<DMatch> matchesList = matches.toList();

				// -- Quick calculation of max and min distances between
				// keypoints
				Double max_dist = 0.0;
				Double min_dist = 100.0;

				for (int i = 0; i < matchesList.size(); i++) {
					Double dist = (double) matchesList.get(i).distance;
					if (dist < min_dist)
						min_dist = dist;
					if (dist > max_dist)
						max_dist = dist;
				}
				// -- Draw only "good" matches (i.e. whose distance is less
				// than 3*min_dist )
				LinkedList<DMatch> good_matches = new LinkedList<DMatch>();
				for (int i = 0; i < matchesList.size(); i++) {
					if (matchesList.get(i).distance <= (3 * min_dist)) // change
																		// the
																		// limit
																		// as
																		// you
																		// desire
						good_matches.addLast(matchesList.get(i));
				}

				Mat outImg = new Mat();
				Features2d.drawMatches(img1, keypoints_object, img2,
						keypoints_scene, matches, outImg,
						new Scalar(0, 255, 0), new Scalar(0, 0, 255),
						new MatOfByte(), Features2d.NOT_DRAW_SINGLE_POINTS);

				// -- Localize the object

				LinkedList<Point> objList = new LinkedList<Point>();
				LinkedList<Point> sceneList = new LinkedList<Point>();

				List<KeyPoint> keypoints_RefList = keypoints_object.toList();
				List<KeyPoint> keypoints_List = keypoints_scene.toList();

				for (int i = 0; i < good_matches.size(); i++) {
					objList.addLast(keypoints_RefList.get(good_matches.get(i).queryIdx).pt);
					sceneList
							.addLast(keypoints_List.get(good_matches.get(i).trainIdx).pt);
				}

				MatOfPoint2f obj = new MatOfPoint2f();
				MatOfPoint2f scene = new MatOfPoint2f();

				obj.fromList(objList);
				scene.fromList(sceneList);

				Mat mask = new Mat();

				Mat hg = Calib3d.findHomography(obj, scene, 8, 10, mask);

				Mat obj_corners = new Mat(4, 1, CvType.CV_32FC2);
				Mat scene_corners = new Mat(4, 1, CvType.CV_32FC2);

				obj_corners.put(0, 0, new double[] { 0, 0 });
				obj_corners.put(1, 0, new double[] { img1.cols(), 0 });
				obj_corners
						.put(2, 0, new double[] { img1.cols(), img1.rows() });
				obj_corners.put(3, 0, new double[] { 0, img1.rows() });

				Core.perspectiveTransform(obj_corners, scene_corners, hg);

				Point p = new Point(scene_corners.get(0, 0));
				double x1 = p.x;
				double y1 = p.y;
				p = new Point(scene_corners.get(1, 0));
				double x2 = p.x;
				double y2 = p.y;
				p = new Point(scene_corners.get(2, 0));
				double x3 = p.x;
				double y3 = p.y;
				// p = new Point(scene_corners.get(3, 0));
				// double x4 = p.x;
				// double y4 = p.y;

				double d1 = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2)
						* (y1 - y2));
				double d2 = Math.sqrt((x2 - x3) * (x2 - x3) + (y2 - y3)
						* (y2 - y3));

				Core.line(img3, new Point(scene_corners.get(0, 0)), new Point(
						scene_corners.get(1, 0)), new Scalar(0, 255, 0), 4);
				Core.line(img3, new Point(scene_corners.get(1, 0)), new Point(
						scene_corners.get(2, 0)), new Scalar(0, 255, 0), 4);
				Core.line(img3, new Point(scene_corners.get(2, 0)), new Point(
						scene_corners.get(3, 0)), new Scalar(0, 255, 0), 4);
				Core.line(img3, new Point(scene_corners.get(3, 0)), new Point(
						scene_corners.get(0, 0)), new Scalar(0, 255, 0), 4);

				Imgproc.cvtColor(img2, img2, Imgproc.COLOR_GRAY2RGBA);
				// Highgui.imwrite(Environment.getExternalStorageDirectory()
				// + "/result_match.jpg", img3);

				Bitmap bmp = Bitmap.createBitmap(500, newheight,
						Config.ARGB_8888);
				//
				Utils.matToBitmap(img3, bmp);

				/*
				 * Toast.makeText( getBaseContext(), "Image contains matches: "
				 * + good_matches.size() + " Area: " + (d1 * d2),
				 * Toast.LENGTH_LONG) .show();
				 */

				if (d1 * d2 > 3000
						|| (good_matches.size() > 4 && good_matches.size() < 100)) {
					// publishProgress(img.getName()
					// + " Image Contains Object. area: " + (d1 * d2)
					// + " size: " + good_matches.size());
					// Toast.makeText(getBaseContext(), "Image contains object",
					// Toast.LENGTH_LONG).show();
					Log.d("selected", img.getParent() + "/" + img.getName());
					String path = img.getPath();// getParentFile().getName();
					selFol.add(img.getParent());
					publishProgress(path);
				}
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {

			post();

			super.onPostExecute(result);
		}

		@Override
		protected void onProgressUpdate(String... values) {
			/*
			 * try { Looper.prepare(); } catch (Exception ex) { } //
			 * Toast.makeText(getBaseContext(), values[0], Toast.LENGTH_LONG) //
			 * .show();
			 */
			File file = new File(values[0]);

			if (Tags.contains(file.getParentFile().getName()))
				return;

			Tags.add(file.getParentFile().getName());
			TagsPath.add(values[0]);

			Button newButton = new Button(ImageSearchActivity.this);
			newButton.setText(file.getParentFile().getName());

			newButton.setTag(values[0]);
			newButton.setMinWidth(ReturnHeight(93, getBaseContext()));
			newButton.setMinHeight(ReturnHeight(50, getBaseContext()));
			newButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
			newButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {

					container.removeView(v);
					Tags.remove(((Button) v).getText());
					try {
						ObjectDatabase db = new ObjectDatabase(
								ImageSearchActivity.this);
						db.open();
						db.updateEntry(v.getTag().toString(), 0, 1);
						db.close();
						TagsPath.remove(v.getTag().toString());
						doWork();
					} catch (Exception ex) {
						Log.d("Minus Update", ex.toString());
					}

				}
			});

			container.addView(newButton);

			super.onProgressUpdate(values);
		}

		@Override
		protected void onPreExecute() {
			try {
				save.setVisible(false);// .setEnabled(false);

			} catch (Exception ex) {
			}
			super.onPreExecute();
		}

	}

	ArrayList<String> Tags = new ArrayList<String>();
	ArrayList<String> TagsPath = new ArrayList<String>();

	String post;

	Handler handler = new Handler();
	Runnable runnable = new Runnable() {

		@Override
		public void run() {
			Toast.makeText(getBaseContext(), post, Toast.LENGTH_SHORT).show();
		}
	};

	@Override
	public void onResume() {
		super.onResume();
		// Toast.makeText(getBaseContext(), "onResume",
		// Toast.LENGTH_LONG).show();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, this,
				mLoaderCallback);
		applyScrollListener();

	}

	ImageLoader imageLoader = ImageLoader.getInstance();

	String[] imageUrls;

	DisplayImageOptions options;

	void doWork() {

		listView = (GridView) findViewById(R.id.result_list);
		listView.setVisibility(View.VISIBLE);

		ArrayList<String> result = new ArrayList<String>();
		ArrayList<String> mainResult = new ArrayList<String>();

		//
		// Log.d("tagit", "" + Tagstl[i]);

		for (int i = 0; i < Tags.size(); i++) {
			try {
				TagsDatabase tb = new TagsDatabase(getBaseContext());
				tb.open();
				result.addAll(tb.CheckIfTagExists(Tags.get(i)));
				tb.close();
			} catch (Exception ex) {
				// Toast.makeText(getBaseContext(), "No results!!",
				// Toast.LENGTH_LONG).show();

				// listView.setVisibility(View.GONE);
				// return;
			}

			// Toast.makeText(getBaseContext(), result.toString(),
			// Toast.LENGTH_LONG).show();
		}

		for (int i = 0; i < result.size(); i++)
			if (!mainResult.contains(result.get(i))) {

				mainResult.add(result.get(i));

			}

		if (mainResult.size() == 0) {
			Toast.makeText(getBaseContext(), "No results!!", Toast.LENGTH_LONG)
					.show();
			listView.setVisibility(View.GONE);
			return;
		}

		Log.d("tagit", mainResult.toString());
		imageUrls = new String[mainResult.size()];
		for (int i = 0; i < mainResult.size(); i++) {
			imageUrls[i] = "file://" + mainResult.get(i);
		}

		options = new DisplayImageOptions.Builder()
				.showStubImage(R.drawable.ic_launcher)
				.showImageForEmptyUri(R.drawable.ic_launcher)
				.showImageOnFail(R.drawable.hash).cacheInMemory().cacheOnDisc()
				.bitmapConfig(Bitmap.Config.RGB_565).build();

		((GridView) listView).setAdapter(new ImageAdapter());
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				startImagePagerActivity(position);
			}
		});

	}

	private void startImagePagerActivity(int position) {
		Intent intent = new Intent(this, ImagePagerActivity.class);
		intent.putExtra(Extra.IMAGES, imageUrls);
		intent.putExtra(Extra.IMAGE_POSITION, position);
		startActivity(intent);
	}

	public class ImageAdapter extends BaseAdapter {
		@Override
		public int getCount() {
			return imageUrls.length;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final ImageView imageView;
			if (convertView == null) {
				imageView = (ImageView) getLayoutInflater().inflate(
						R.layout.item_grid_image, parent, false);
			} else {
				imageView = (ImageView) convertView;
			}

			imageLoader.displayImage(imageUrls[position], imageView, options);

			return imageView;
		}
	}

	private void applyScrollListener() {
		try {
			listView.setOnScrollListener(new PauseOnScrollListener(imageLoader,
					pauseOnScroll, pauseOnFling));
		} catch (Exception ex) {
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(STATE_PAUSE_ON_SCROLL, pauseOnScroll);
		outState.putBoolean(STATE_PAUSE_ON_FLING, pauseOnFling);
	}

	protected static final String STATE_PAUSE_ON_SCROLL = "STATE_PAUSE_ON_SCROLL";
	protected static final String STATE_PAUSE_ON_FLING = "STATE_PAUSE_ON_FLING";

	protected AbsListView listView;

	protected boolean pauseOnScroll = false;
	protected boolean pauseOnFling = true;

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		pauseOnScroll = savedInstanceState.getBoolean(STATE_PAUSE_ON_SCROLL,
				false);
		pauseOnFling = savedInstanceState
				.getBoolean(STATE_PAUSE_ON_FLING, true);
	}

}
