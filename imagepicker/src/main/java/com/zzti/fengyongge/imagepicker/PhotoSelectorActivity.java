package com.zzti.fengyongge.imagepicker;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.zzti.fengongge.imagepicker.R;
import com.zzti.fengyongge.imagepicker.adapter.AlbumAdapter;
import com.zzti.fengyongge.imagepicker.adapter.PhotoSelectorAdapter;
import com.zzti.fengyongge.imagepicker.control.PhotoSelectorDomain;
import com.zzti.fengyongge.imagepicker.model.AlbumModel;
import com.zzti.fengyongge.imagepicker.model.PhotoModel;
import com.zzti.fengyongge.imagepicker.util.AnimationUtils;
import com.zzti.fengyongge.imagepicker.util.CommonUtils;
import com.zzti.fengyongge.imagepicker.util.FileProviderUtil;
import com.zzti.fengyongge.imagepicker.util.FileUtils;
import com.zzti.fengyongge.imagepicker.util.ImageUtils;
import com.zzti.fengyongge.imagepicker.util.StringUtils;
import com.zzti.fengyongge.imagepicker.view.SelectPhotoItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;


/**
 * Created by fengyongge on 2016/5/24
 * 图片选择
 */
public class PhotoSelectorActivity extends Activity implements SelectPhotoItem.onItemClickListener, SelectPhotoItem.onPhotoItemCheckedListener,
        OnItemClickListener, OnClickListener
        , EasyPermissions.PermissionCallbacks {

    private static final int WRITE_EXTERNAL_STORAGE = 123;
    private static final int RC_CAMERA_PERM = 124;
    private static final int RC_SETTINGS_SCREEN = 125;


    private ImageView ivBack;
    private TextView tvPercent, tvTitle;
    public static final String RECCENT_PHOTO = "最近照片";
    private List<PhotoModel> single_photos = new ArrayList<PhotoModel>();
    private GridView gvPhotos;
    private PhotoSelectorAdapter photoAdapter;
    private ListView lvAblum;
    private AlbumAdapter albumAdapter;
    private TextView tvAlbum, tvPreview;
    private PhotoSelectorDomain photoSelectorDomain;
    private RelativeLayout layoutAlbum;
    public static ArrayList<PhotoModel> selected = new ArrayList<PhotoModel>();
    private ArrayList<String> imagePathList = new ArrayList<String>();
    private String pathName;
    private boolean isShowCamera;
    private int limit;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            Intent data = new Intent();
            Bundle bundle = new Bundle();
            bundle.putSerializable("photos", imagePathList);
            data.putExtras(bundle);
            setResult(RESULT_OK, data);
            finish();
        }

        ;
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_photoselector);
        initView();
        initOnclick();
        initImageLoader();
        writeTask();
    }

    void initView() {
        ivBack = findViewById(R.id.ivBack);
        tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText("最近照片");
        tvPercent = findViewById(R.id.tvPercent);
        tvPercent.setBackgroundResource(R.drawable.shape_gray_bg);
        gvPhotos = findViewById(R.id.gv_photos_ar);
        lvAblum = findViewById(R.id.lv_ablum_ar);
        tvAlbum = findViewById(R.id.tv_album_ar);
        tvPreview = findViewById(R.id.tv_preview_ar);
        layoutAlbum = findViewById(R.id.layout_album_ar);
        isShowCamera = getIntent().getBooleanExtra(ImagePickerInstance.IS_SHOW_CAMERA, true);
        limit = getIntent().getIntExtra(ImagePickerInstance.LIMIT, 0);
        photoSelectorDomain = new PhotoSelectorDomain(getApplicationContext());
        initPercent();
    }

    void initOnclick() {
        ivBack.setOnClickListener(this);
        tvPercent.setOnClickListener(this);
        tvAlbum.setOnClickListener(this);
        tvPreview.setOnClickListener(this);
    }


    void initPercent(){
        tvPercent.setText("完成");
        tvPercent.setBackgroundResource(R.drawable.shape_gray_bg);
        tvPercent.setTextColor(ContextCompat.getColor(PhotoSelectorActivity.this, R.color.gray_69));
    }



    public void initImageLoader() {
        DisplayImageOptions defaultDisplayImageOptions = new DisplayImageOptions.Builder()
                // 调整图片方向
                .considerExifParams(true)
                // 载入之前重置ImageView
                .resetViewBeforeLoading(true)
                // 载入时图片设置为黑色
                .showImageOnLoading(R.drawable.ic_picture_loading)
                // 加载失败时显示的图片
                .showImageOnFail(R.drawable.ic_picture_loadfailed)
                // 载入之前的延迟时间
                .delayBeforeLoading(0)
                .build();
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
                .defaultDisplayImageOptions(defaultDisplayImageOptions).memoryCacheExtraOptions(480, 800)
                .threadPoolSize(5).build();
        ImageLoader.getInstance().init(config);
    }


    public void showPic() {
        photoAdapter = new PhotoSelectorAdapter(getApplicationContext(), new ArrayList<PhotoModel>(),
                CommonUtils.getWidthPixels(this), this, this, this, limit);
        gvPhotos.setAdapter(photoAdapter);
        albumAdapter = new AlbumAdapter(getApplicationContext(), new ArrayList<AlbumModel>());
        lvAblum.setAdapter(albumAdapter);
        lvAblum.setOnItemClickListener(this);
        // 更新最近照片
        photoSelectorDomain.getReccent(reccentListener);
        // 更新相册信息
        photoSelectorDomain.updateAlbum(albumListener);
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (v.getId() == R.id.ivBack) {
            finish();
        } else if (id == R.id.tvPercent) {
            ok();
        } else if (id == R.id.tv_album_ar) {
            album();
        } else if (v.getId() == R.id.tv_preview_ar) {
            priview();
        } else if (v.getId() == R.id.rlCamera) {
            cameraTask();
        }
    }

    /**
     * 拍照
     */
    private void catchPicture() {
        pathName = "image" + (Math.round((Math.random() * 9 + 1) * 100000)) + ".jpg";
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // 下面这句指定调用相机拍照后的照片存储的路径
        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                FileProviderUtil.getFileUri(PhotoSelectorActivity.this,
                        new File(Environment.getExternalStorageDirectory(),
                                pathName),
                        this.getPackageName() + ".fileprovider"));
        startActivityForResult(intent, 2);
    }

    /**
     * 复制图片
     */
    private String cropImage(Bitmap cropImage) {
        if (cropImage == null) {
            return null;
        } else {
            File appDir = new File(Environment.getExternalStorageDirectory(), "imagePicker");
            if (!appDir.exists()) {
                appDir.mkdir();
            }
            String pathName = appDir.getAbsolutePath() + File.separator + System.currentTimeMillis() + ".jpg";
            FileUtils.writeImage(cropImage, pathName, 100);
            return pathName;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 2) {
            if (resultCode == -1) {
                File takefile = new File(Environment.getExternalStorageDirectory(), pathName);
                PhotoModel photoModel = new PhotoModel(takefile.getAbsolutePath());
                selected.clear();
                selected.add(photoModel);
                ok();
            }
        }
    }


    /**
     * 完成
     */
    private void ok() {
        if (selected.isEmpty()) {
            setResult(RESULT_CANCELED);
            finish();
        } else {
            imagePathList.clear();
            new Thread() {
                private String cropImage;

                @Override
                public void run() {
                    for (int i = 0; i < selected.size(); i++) {
                        //防止拍照图片角度发生变化(三星)
                        int degree = ImageUtils.getBitmapDegree(selected.get(i).getOriginalPath());
                        if (degree == 0) {
                            cropImage = cropImage(ImageUtils.getimage(selected.get(i).getOriginalPath()));
                        } else {
                            cropImage = cropImage(ImageUtils.rotateBitmapByDegree(ImageUtils.getimage(selected.get(i).getOriginalPath()), degree));
                        }
                        if (StringUtils.isNotEmpty(cropImage)) {
                            imagePathList.add(cropImage);
                        }
                    }
                    handler.sendEmptyMessage(0);
                }
            }.start();
        }
    }

    /**
     * 预览照片
     */
    private void priview() {
        Bundle bundle = new Bundle();
        bundle.putSerializable("photos", selected);
        CommonUtils.launchActivity(this, PhotoPreviewActivity.class, bundle);
    }

    private void album() {
        if (layoutAlbum.getVisibility() == View.GONE) {
            popAlbum();
        } else {
            hideAlbum();
        }
    }

    /**
     * 弹出相册列表
     */
    private void popAlbum() {
        layoutAlbum.setVisibility(View.VISIBLE);
        new AnimationUtils(getApplicationContext(), R.anim.translate_up_current).setLinearInterpolator().startAnimation(
                layoutAlbum);
    }

    /**
     * 隐藏相册列表
     */
    private void hideAlbum() {
        new AnimationUtils(getApplicationContext(), R.anim.translate_down).setLinearInterpolator().startAnimation(
                layoutAlbum);
        layoutAlbum.setVisibility(View.GONE);
    }

    /**
     * 清空选中的图片
     */
    private void reset() {
        selected.clear();
        tvPreview.setText("预览");
        tvPreview.setEnabled(false);
    }

    @Override
    /** 点击查看照片 */
    public void onItemClick(int position) {
        Bundle bundle = new Bundle();
        if (tvAlbum.getText().toString().equals(RECCENT_PHOTO)) {
            bundle.putInt("position", position - 1);
        } else {
            bundle.putInt("position", position);
        }
        bundle.putString("album", tvAlbum.getText().toString());
        CommonUtils.launchActivity(this, PhotoPreviewActivity.class, bundle);
    }


    @Override
    /** 照片选中状态改变之后 */
    public void onCheckedChanged(PhotoModel photoModel, CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            selected.add(photoModel);
            tvPreview.setEnabled(true);
        } else {
            selected.remove(photoModel);
        }
        tvPreview.setText("预览(" + selected.size() + ")");
        updatePercent(selected.size(), limit);
        if (selected.isEmpty()) {
            tvPreview.setEnabled(false);
            tvPreview.setText("预览");
        }
    }

    @Override
    public void onBackPressed() {
        if (layoutAlbum.getVisibility() == View.VISIBLE) {
            hideAlbum();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    /** 相册列表点击事件 */
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        AlbumModel current = (AlbumModel) parent.getItemAtPosition(position);
        for (int i = 0; i < parent.getCount(); i++) {
            AlbumModel album = (AlbumModel) parent.getItemAtPosition(i);
            if (i == position) {
                album.setCheck(true);
            } else {
                album.setCheck(false);
            }
        }
        albumAdapter.notifyDataSetChanged();
        hideAlbum();
        tvAlbum.setText(current.getName());
        tvTitle.setText(current.getName());

        // 更新照片列表
        if (current.getName().equals(RECCENT_PHOTO)) {
            photoSelectorDomain.getReccent(reccentListener);
        } else {
            // 获取选中相册的照片
            photoSelectorDomain.getAlbum(current.getName(), reccentListener);
        }
    }

    /**
     * 展示当前张数
     */
    protected void updatePercent(int current, int totleNum) {
        if (current > 0) {
            tvPercent.setText("完成(" + current + "/" + totleNum + ")");
            tvPercent.setBackgroundResource(R.drawable.shape_green_bg);
            tvPercent.setTextColor(ContextCompat.getColor(PhotoSelectorActivity.this, R.color.white));
        } else {
            initPercent();
        }
    }

    /**
     * 获取本地图库照片回调
     */
    public interface OnLocalReccentListener {
        void onPhotoLoaded(List<PhotoModel> photos);
    }

    /**
     * 获取本地相册信息回调
     */
    public interface OnLocalAlbumListener {
        void onAlbumLoaded(List<AlbumModel> albums);
    }

    private OnLocalAlbumListener albumListener = new OnLocalAlbumListener() {
        @Override
        public void onAlbumLoaded(List<AlbumModel> albums) {
            albumAdapter.update(albums);
        }
    };
    private OnLocalReccentListener reccentListener = new OnLocalReccentListener() {
        @Override
        public void onPhotoLoaded(List<PhotoModel> photos) {

            if(isShowCamera){
                photos.add(0, new PhotoModel());
            }
            single_photos.clear();
            single_photos.addAll(photos);
            photoAdapter.update(photos);
            gvPhotos.smoothScrollToPosition(0); // 滚动到顶端
            reset();
        }
    };


    /**
     * 6.0动态申请获取相册权限
     */
    @AfterPermissionGranted(WRITE_EXTERNAL_STORAGE)
    public void writeTask() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // Have permission, do the thing!
            showPic();
        } else {
            // Ask for one permission
            EasyPermissions.requestPermissions(this, getString(R.string.write),
                    WRITE_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }


    /**
     * 6.0动态申请拍照权限
     */
    @AfterPermissionGranted(RC_CAMERA_PERM)
    public void cameraTask() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA)) {
            // Have permission, do the thing!
            catchPicture();
        } else {
            // Ask for one permission
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_camera),
                    RC_CAMERA_PERM, Manifest.permission.CAMERA);
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        switch (requestCode) {
            case WRITE_EXTERNAL_STORAGE:
                showPic();
                break;
            case RC_CAMERA_PERM:
                catchPicture();
                break;
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        // (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        // This will display a dialog directing them to enable the permission in app settings.
        new AppSettingsDialog.Builder(PhotoSelectorActivity.this)
                .setTitle(getString(R.string.title_settings_dialog))
                .setPositiveButton(getString(R.string.setting))
                .setNegativeButton(getString(R.string.cancel))
                .setRequestCode(RC_SETTINGS_SCREEN)
                .build()
                .show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 调用EasyPermissions的onRequestPermissionsResult方法，参数和系统方法保持一致，然后就不要关心具体的权限申请代码了
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

}
