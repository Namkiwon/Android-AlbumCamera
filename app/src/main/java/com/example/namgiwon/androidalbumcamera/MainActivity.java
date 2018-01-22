package com.example.namgiwon.androidalbumcamera;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSION_CAMERA=1111;
    private static final int REQUEST_TAKE_PHOTO=2222;
    private static final int REQUEST_TAKE_ALBUM = 3333;
    private static final int REQUEST_IMAGE_CROP=4444;
    String imageFileName;
    String imgPath = "";
    Uri albumUri;
    Uri photoUri;
    Uri imageUri;
    Bitmap bitimage;
    ImageView bufferimage;
    File photoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bufferimage = (ImageView) findViewById(R.id.bufferimage);
        bufferimage.setOnClickListener(bListener);
        checkPremission();
    }

    Button.OnClickListener bListener = new Button.OnClickListener(){
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.bufferimage:
                    checkPremission();
//                    getAlbum();
                    captureCamera();
                    break;
            }
        }
    };

    private void captureCamera(){
        String state = Environment.getExternalStorageState();
        //외장메모리 검사
        if(Environment.MEDIA_MOUNTED.equals(state)){
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            if(takePictureIntent.resolveActivity(getPackageManager()) != null){
                photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException e) {
                    Log.e("captureCamera Error",e.toString());
                }
                if(photoFile != null){
                    //getURiForFile 의 두번째 인자는 Manifest provider의 authoites와 일치해야함
                    Log.d("asdfasdf",getPackageName());
                    Uri providerURI = FileProvider.getUriForFile(this,getPackageName(),photoFile);
                    photoUri = providerURI;

                    //인텐트에 전달할 때는 FileProvider의 Return값인 content://로만!!
                    //providerURI의 값에 카메라 데이터를 넣어보냄
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,providerURI);
                    startActivityForResult(takePictureIntent,REQUEST_TAKE_PHOTO);
                }
            }
        }else{
            Toast.makeText(this,"저장공간 접근이 불가능한 기기입니다",Toast.LENGTH_LONG).show();
            return;
        }
    }


    private void galleryAddPic(){
        Log.i("galleryAddPic","Call");
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(imgPath);
        Uri contentUri = Uri.fromFile(f);
        sendBroadcast(mediaScanIntent);
        Toast.makeText(this,"사진이 앨범에 저장되었습니다.",Toast.LENGTH_LONG).show();
    }


    private void checkPremission(){
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            if((ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE))||
                    (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.CAMERA))){
                new AlertDialog.Builder(this)
                        .setTitle("알림")
                        .setMessage("저장소 권한 거부")
                        .setNegativeButton("설정",new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent i =new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                i.setData(Uri.parse("package"+getPackageName()));
                                startActivity(i);

                            }
                        })
                        .setPositiveButton("확인",new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .setCancelable(false)
                        .create()
                        .show();

            }else {
                ActivityCompat.requestPermissions(this,new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.CAMERA},MY_PERMISSION_CAMERA);
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case MY_PERMISSION_CAMERA:
                for(int i=0; i<grantResults.length;i++){
                    if(grantResults[i]<0){
                        Toast.makeText(MainActivity.this,"해당 권한을 활성화 해야합니다.",Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                break;
        }
    }

    public static Bitmap imgRotate(Bitmap bmp, int orientation){
        int width = bmp.getWidth();
        int height = bmp.getHeight();

        Matrix matrix = new Matrix();
        matrix.postRotate(orientation);

        Bitmap resizedBitmap = Bitmap.createBitmap(bmp, 0, 0, width, height, matrix, true);
        bmp.recycle();
        return resizedBitmap;
    }

    public File createImageFile()throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        imageFileName = timeStamp+".jpg";

        File imageFile = null;
        File storageDir = new File(Environment.getExternalStorageDirectory()+"/Pictures","nam");

        if(!storageDir.exists()){
            storageDir.mkdirs();
        }
        imageFile = new File(storageDir, imageFileName);
        imgPath = imageFile.getAbsolutePath();

        Log.d("-------storageDir","create file path : "+imgPath);
        return imageFile;
    }
    private void getAlbum(){
        Log.i("getAlbum","Call");
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
        startActivityForResult(intent,REQUEST_TAKE_ALBUM);
    }

    public void cropImage(){
        Log.i("cropImage","Call");
        Log.i("cropImage","photoURI:"+photoUri+" /albumURI : "+albumUri);

        Intent cropIntent = new Intent("com.android.camera.action.CROP");
        cropIntent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        cropIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        cropIntent.setDataAndType(photoUri,"image/*");
        cropIntent.putExtra("aspectX",1);
        cropIntent.putExtra("aspectY",1);
        cropIntent.putExtra("output",albumUri);
        startActivityForResult(cropIntent,REQUEST_IMAGE_CROP);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case REQUEST_TAKE_PHOTO:
                if(resultCode == Activity.RESULT_OK){
                    try{
                        Log.i("REQUEST_TAKE_PHOTO","OK");
                        galleryAddPic();
                        albumUri=Uri.fromFile(photoFile);
                        cropImage();
////                        bufferimage.setImageURI(photoUri);
//
//                        // 사진의 회전된 정도를 구함
//                        Bitmap bm = MediaStore.Images.Media.getBitmap(getContentResolver(),photoUri);
//                        //찍으면서 사진이 돌아간만큼 다시 돌림
//                        bm = imgRotate(bm,getImageOrientation(imgPath));
//                        bufferimage.setImageBitmap(bm);
                    }catch (Exception e){
                        Log.e("REQUEST_TAKE_PHOTO",e.toString());
                    }
                }else{
                    Toast.makeText(MainActivity.this,"사진찍기를 취소하였습니다.",Toast.LENGTH_LONG).show();
                }
                break;
            case REQUEST_TAKE_ALBUM:
                if(resultCode== Activity.RESULT_OK){
                    if(data.getData()!=null){
                        try{
                            File albumFile = null;
                            albumFile = createImageFile();
                            photoUri=data.getData();
                            albumUri=Uri.fromFile(albumFile);
                            cropImage();
                        }catch (Exception e){
                            Log.e("Take_ALBUM_SINGLE ERROR",e.toString());
                        }
                    }
                }
                break;
            case REQUEST_IMAGE_CROP:
                if(resultCode==Activity.RESULT_OK){
                    // savePicture();
                    bufferimage.setImageURI(albumUri);
                    System.out.println(imgPath+"asdfasdfasdfasdfasdfasdfasdfasdfasfd");
                }
                break;
        }

    }

    public static int getImageOrientation(String path){

        int rotation =0;
        try {
            ExifInterface exif = new ExifInterface(path);
            int rot= exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            if(rot == ExifInterface.ORIENTATION_ROTATE_90){
                rotation = 90;
            }else if(rot == ExifInterface.ORIENTATION_ROTATE_180){
                rotation = 180;
            }else if(rot == ExifInterface.ORIENTATION_ROTATE_270){
                rotation = 270;
            }else{
                rotation = 0;
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return rotation;
    }


}
