package beproject.cbir;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import beproject.cbir.database.ObjectDatabase;
import beproject.cbir.database.TagsDatabase;
import beproject.cbir.smoothprogressbar.SmoothProgressBar;

@SuppressLint("NewApi")
public class TagActivity extends Activity {

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
				if (once)
					new LetsTagThem().execute(bitmap);
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			getActionBar().setBackgroundDrawable(
					getResources().getDrawable(R.drawable.actionbar_back));

		}

		// this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.setContentView(R.layout.tag);

		if (!getIntent().hasExtra("ImgPath")) {
			Toast.makeText(getBaseContext(), "Error Occurred!!",
					Toast.LENGTH_LONG).show();
			finish();
		}

		bitmap = BitmapFactory
				.decodeFile(getIntent().getStringExtra("ImgPath"));

		imagePath = getIntent().getStringExtra("ImgPath");

		setTypeFace(((TextView) findViewById(R.id.tvforTags)), "cnl.ttf");

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

		Bitmap bmps = Bitmap.createBitmap(bitmap.getWidth(),
				bitmap.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(bmps);
		canvas.drawColor(Color.BLACK);
		((ImageView) findViewById(R.id.ivback)).setImageBitmap(bmps);

		// bitmap = ;

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

		// Toast.makeText(getBaseContext(), "onCreate",
		// Toast.LENGTH_LONG).show();

		// new LetsTagThem().execute(bitmap);

		// traverse(dir);

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

	MenuItem save, add;

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

		add = menu.add("Add more Tags");
		add.setIcon(android.R.drawable.ic_menu_add);
		add.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		add.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				save();
				Bundle b = new Bundle();
				b.putStringArrayList("tags", Tags);
				b.putStringArrayList("tagsPath", TagsPath);
				startActivity(new Intent(getBaseContext(), TrainActivity.class)
						.putExtra("ImgPath", imagePath)
						.putExtra("tagBundle", b));
				finish();

				return true;
			}
		});

		// MenuItem help = menu.add("Help");
		// help.setIcon(android.R.drawable.ic_menu_help);
		// help.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		if (Tags.size() > 0) {
			save.setVisible(true);
			add.setVisible(true);
		} else {
			save.setVisible(false);
			add.setVisible(false);
		}
		return super.onCreateOptionsMenu(menu);
	}

	void setTypeFace(View v, String tfs) {
		Typeface tf = Typeface.createFromAsset(getAssets(), tfs);
		if (v instanceof Button || v instanceof TextView
				|| v instanceof EditText) {
			((TextView) v).setTypeface(tf);
		}
	}

	public void save() {

		TagsDatabase db = new TagsDatabase(TagActivity.this);
		db.open();
		String tag = " ";
		for (int i = 0; i < Tags.size(); i++)
			tag = tag + Tags.get(i) + " , ";
		db.CreateOrUpdateEntry(imagePath, tag);
		db.close();

		ObjectDatabase odb = new ObjectDatabase(TagActivity.this);

		odb.open();

		for (int i = 0; i < TagsPath.size(); i++) {
			odb.updateEntry(TagsPath.get(i), 1, 1);
		}

		odb.close();

		for (int i = 0; i < Tags.size(); i++) {
			try {
				copyFile(new File(newTags.get(i)),
						new File(Environment.getExternalStorageDirectory()
								+ "/TagIt/.Tags/" + Tags.get(i)));
				new File(newTags.get(i)).delete();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		Toast.makeText(getBaseContext(), "Image saved to database!!",
				Toast.LENGTH_LONG).show();
	}

	public void copyFile(File sourceFile, File targetLocation)
			throws IOException {

		if (!targetLocation.exists()) {
			targetLocation.mkdirs();
		}
		// File[] files = sourceLocation.listFiles();
		// for (File file : files) {
		InputStream in = new FileInputStream(sourceFile);
		OutputStream out = new FileOutputStream(targetLocation + "/TS_"
				+ System.currentTimeMillis() + ".jpg");
		// String path = targetLocation + "/TS_" + System.currentTimeMillis() +
		// ".jpg";

		// Copy the bits from input stream to output stream
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
		// }

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

				double x[] = new double[4];
				double y[] = new double[4];

				double x1 = p.x;
				double y1 = p.y;
				x[0] = x1;
				y[0] = y1;
				p = new Point(scene_corners.get(1, 0));
				double x2 = p.x;
				double y2 = p.y;
				x[1] = x2;
				y[1] = y2;
				p = new Point(scene_corners.get(2, 0));
				double x3 = p.x;
				double y3 = p.y;
				x[2] = x3;
				y[2] = y3;
				p = new Point(scene_corners.get(3, 0));
				double x4 = p.x;
				double y4 = p.y;
				x[3] = x4;
				y[3] = y4;

				double minx = 9999, miny = 9999, maxx = -99, maxy = -99;
				for (int i = 0; i < 4; i++) {
					if (x[i] < minx)
						minx = x[i];
					if (y[i] < miny)
						miny = y[i];
					if (x[i] > maxx)
						maxx = x[i];
					if (y[i] > maxy)
						maxy = y[i];
				}

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

					if (!Tags
							.contains(new File(path).getParentFile().getName())) {

						try {

							int mheight = (int) (maxx - minx);
							int mwidth = (int) (maxy - miny);

							int nheight = 200 * (mheight / mwidth);

							if (nheight < 1) {
								nheight = 200;
							}

							Bitmap mBitmap = Bitmap.createBitmap(200, nheight,
									Config.ARGB_8888);
							Canvas mCanvas = new Canvas(mBitmap);
							Paint mPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
							mPaint.setAntiAlias(true);
							mCanvas.drawBitmap(
									bmpimg2,
									new Rect((int) minx, (int) miny,
											(int) maxx, (int) maxy),
									new RectF(0, 0, mBitmap.getWidth(), mBitmap
											.getHeight()), mPaint);
							FileOutputStream outs = null;

							File dirs = new File(
									Environment.getExternalStorageDirectory()
											+ "/TagIt/.temp/");
							if (!dirs.exists())
								dirs.mkdirs();

							newTags.add(Environment
									.getExternalStorageDirectory()
									+ "/TagIt/"
									+ ".temp/TS_temp_"
									+ System.currentTimeMillis() + ".jpg");

							outs = new FileOutputStream(new File(
									Environment.getExternalStorageDirectory()
											+ "/TagIt/" + ".temp/TS_temp_"
											+ System.currentTimeMillis()
											+ ".jpg"));
							mBitmap.compress(Bitmap.CompressFormat.JPEG, 100,
									outs);
						} catch (Exception ex) {
						}
					}
				}
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {

			((SmoothProgressBar) findViewById(R.id.smoothProgressBar1))
					.setVisibility(View.GONE);
			((ProgressBar) findViewById(R.id.progressBar1))
					.setVisibility(View.GONE);
			// Toast.makeText(getBaseContext(), "Done!!", Toast.LENGTH_LONG)
			// .show();

			((TextView) findViewById(R.id.tvforTags)).setText("Tags:");
			save.setVisible(true);
			add.setVisible(true);
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

			Button newButton = new Button(TagActivity.this);
			newButton.setText(file.getParentFile().getName());

			newButton.setTag(values[0]);
			newButton.setMinWidth(ReturnHeight(93, getBaseContext()));
			newButton.setMinHeight(ReturnHeight(50, getBaseContext()));
			newButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
			newButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {

					container.removeView(v);
					int index = Tags.indexOf(((Button) v).getText());
					Tags.remove(((Button) v).getText());
					try {

						new File(newTags.remove(index)).delete();

						ObjectDatabase db = new ObjectDatabase(TagActivity.this);
						db.open();
						db.updateEntry(v.getTag().toString(), 0, 1);
						db.close();
						TagsPath.remove(v.getTag().toString());

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
				add.setVisible(false);
			} catch (Exception ex) {
			}
			super.onPreExecute();
		}

	}

	ArrayList<String> Tags = new ArrayList<String>();
	ArrayList<String> TagsPath = new ArrayList<String>();
	ArrayList<String> newTags = new ArrayList<String>();

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

	}

}
