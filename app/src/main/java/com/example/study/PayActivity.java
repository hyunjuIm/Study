package com.example.study;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Browser;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.android.volley.toolbox.Volley;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PayActivity extends AppCompatActivity{
    private static PayActivity instance;

    private WebView webPay;

    private String TAG = "CAN";

    // 카드사 승인번호, 거래번호, 주문번호
    private String authno, trno, orderno;

    private String validationPrice = "";
    private String validationTid = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay);
        instance = this;

        initView();
    }

    @SuppressLint("JavascriptInterface")
    private void initView() {
        webPay = (WebView) findViewById(R.id.webPay);

        webPay.getSettings().setJavaScriptEnabled(true);

        // android 5.0부터 앱에서 API수준21이상을 타겟킹하는 경우 아래추가
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webPay.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            cookieManager.setAcceptThirdPartyCookies(webPay, true);
        }

        webPay.addJavascriptInterface(new PayActivity.PayBridge(), "PayApp");
        webPay.setWebViewClient(new LguplusWebClient());
        webPay.setWebChromeClient(new LguplusWebChromeClient());

        webPay.loadUrl("http://www.sejongbike.kr/appserver/pay_lguplus/Loading.html");
    }

    private class PayBridge {
        @JavascriptInterface
        public void Result(final String result, final String authno, final String trno, final String ordno) {
            // 구매완료
            try {
                // authno : 카드사 승인번호, trno : 거래번호, orderno : 주문번호
                Log.d(TAG, "PayBridge, Result() : result : " + result + ", authno : " + authno + ", trno : " + trno + ", ordno : " + ordno);

                if (result != null && authno != null && trno != null && ordno != null) {
                    if (result.trim().equals("0000")) {
                        instance.authno = authno.trim();
                        instance.trno = trno.trim();
                        instance.orderno = ordno.trim();
                    } else {
                        Toast.makeText(PayActivity.this, "취소", Toast.LENGTH_LONG);
                    }
                } else {
                    Toast.makeText(PayActivity.this, "취소", Toast.LENGTH_LONG);
                }
            } catch (Exception e) {
                Log.d(TAG, instance.getClass().getSimpleName() + " -> " + "PayBridge : Result()");
                Toast.makeText(PayActivity.this, "취소", Toast.LENGTH_LONG);
            }
        }

        @JavascriptInterface
        public void Result(final String result, final String authno, final String trno, final String ordno, final String tid, final String amount) {
            // 구매완료
            try {
                // authno : 카드사 승인번호, trno : 거래번호, orderno : 주문번호
                Log.d(TAG, "PayBridge, Result() : result : " + result + ", authno : " + authno + ", trno : " + trno + ", ordno : " + ordno);

                if (result != null && authno != null && trno != null && ordno != null && tid != null && amount != null) {
                    validationTid = tid;
                    if (result.trim().equals("0000") && validationPrice.equals(amount)) {
                        instance.authno = authno.trim();
                        instance.trno = trno.trim();
                        instance.orderno = ordno.trim();
                    } else if (result.trim().equals("0000")) {
                        //Cancel Call
                        Toast.makeText(PayActivity.this, "취소", Toast.LENGTH_LONG);
                    } else {
                        Toast.makeText(PayActivity.this, "취소", Toast.LENGTH_LONG);
                    }
                } else {
                    Toast.makeText(PayActivity.this, "취소", Toast.LENGTH_LONG);
                }
            } catch (Exception e) {
                Log.d(TAG, instance.getClass().getSimpleName() + " -> " + "PayBridge : Result()");
                Toast.makeText(PayActivity.this, "취소", Toast.LENGTH_LONG);
            }
        }

        @JavascriptInterface
        public void Cancel() {
            Toast.makeText(PayActivity.this, "취소", Toast.LENGTH_LONG);
        }

        @JavascriptInterface
        public void GetUserData() {
            // 사용자 데이터 보내기
            String payment_kind = "SC0010";//결제타입 - SC0060(폰결제), SC0010(카드결제)
            String userName = "임현주"; // 성명
            String userId = "abc"; // ID
            String userEmail = "dear_jjwim@naver.com"; // email
            String orderNumber = "0000"; // 주문번호
            String goodsName = "정기권"; // 상품명
            String price = "10"; // 가격

            webPay.post(new Runnable() {
                @Override
                public void run() {
                    String url = "javascript:userData('" + payment_kind + "', '" + price + "', '" + userName + "', '"
                            + orderNumber + "', '" + goodsName + "', '" + userId + "', '" + userEmail + "')";
                    Log.d(TAG, "url : " + url);
                    webPay.loadUrl(url);
                }
            });
        }
    }

    private class LguplusWebClient extends WebViewClient {
        public boolean shouldOverrideUrlLoading(final WebView view, String url) {
                    if ((url.startsWith("http://") || url.startsWith("https://")) && url.endsWith(".apk")) {
                        downloadFile(url);
                        return super.shouldOverrideUrlLoading(view, url);
                    } else if ((url.startsWith("http://") || url.startsWith("https://")) &&
                            (url.contains("market.android.com") || url.contains("m.ahnlab.com/kr/site/download"))) {
                        Uri uri = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    startActivity(intent);
                    return true;
                } catch (ActivityNotFoundException e) {
                    return false;
                }
            } else if (url.startsWith("http://") || url.startsWith("https://")) {
                view.loadUrl(url);
                return true;
            } else if (url != null &&
                    (url.contains("vguard")
                            || url.contains("droidxantivirus")
                            || url.contains("smhyundaiansimclick://")
                            || url.contains("smshinhanansimclick://")
                            || url.contains("smshinhancardusim://")
                            || url.contains("smartwall://")
                            || url.contains("appfree://")
                            || url.contains("v3mobile")
                            || url.endsWith(".apk")
                            || url.contains("market://")
                            || url.contains("ansimclick")
                            || url.contains("market://details?id=com.shcard.smartpay")
                            || url.contains("shinhan-sr-ansimclick://"))) {
                return callApp(url);
            } else if (url.startsWith("smartxpay-transfer://")) {
                boolean isatallFlag = isPackageInstalled(getApplicationContext(), "kr.co.uplus.ecredit");
                if (isatallFlag) {
                    boolean override = false;
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.addCategory(Intent.CATEGORY_BROWSABLE);
                    intent.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());

                    try {
                        startActivity(intent);
                        override = true;
                    } catch (ActivityNotFoundException ex) {
                    }
                    return override;
                } else {
                    showAlert("확인버튼을 누르시면 구글플레이로 이동합니다.", "확인", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(("market://details?id=kr.co.uplus.ecredit")));
                            intent.addCategory(Intent.CATEGORY_BROWSABLE);
                            intent.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());
                            startActivity(intent);
                            overridePendingTransition(0, 0);
                        }
                    }, "취소", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    return true;
                }
            } else if (url.startsWith("ispmobile://")) {
                boolean isatallFlag = isPackageInstalled(getApplicationContext(), "kvp.jjy.MispAndroid320");
                if (isatallFlag) {
                    boolean override = false;
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.addCategory(Intent.CATEGORY_BROWSABLE);
                    intent.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());

                    try {
                        startActivity(intent);
                        override = true;
                    } catch (ActivityNotFoundException ex) {
                    }
                    return override;
                } else {
                    showAlert("확인버튼을 누르시면 구글플레이로 이동합니다.", "확인", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            view.loadUrl("http://mobile.vpay.co.kr/jsp/MISP/andown.jsp");
                        }
                    }, "취소", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    return true;
                }
            } else if (url.startsWith("paypin://")) {
                boolean isatallFlag = isPackageInstalled(getApplicationContext(), "com.skp.android.paypin");
                if (isatallFlag) {
                    boolean override = false;
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.addCategory(Intent.CATEGORY_BROWSABLE);
                    intent.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());

                    try {
                        startActivity(intent);
                        override = true;
                    } catch (ActivityNotFoundException ex) {
                    }
                    return override;
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse(("market://details?id=com.skp.android.paypin&feature=search_result#?t=W251bGwsMSwxLDEsImNvbS5za3AuYW5kcm9pZC5wYXlwaW4iXQ..")));
                    intent.addCategory(Intent.CATEGORY_BROWSABLE);
                    intent.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                }
            } else if (url.startsWith("lguthepay://")) {
                boolean isatallFlag = isPackageInstalled(getApplicationContext(), "com.lguplus.paynow");
                if (isatallFlag) {
                    boolean override = false;
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.addCategory(Intent.CATEGORY_BROWSABLE);
                    intent.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());

                    try {
                        startActivity(intent);
                        override = true;
                    } catch (ActivityNotFoundException ex) {
                    }
                    return override;
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(("market://details?id=com.lguplus.paynow")));
                    intent.addCategory(Intent.CATEGORY_BROWSABLE);
                    intent.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    return true;
                }
            } else {
                return callApp(url);
            }
        }

        // DownloadFileTask생성 및 실행
        private void downloadFile(String mUrl) {
            new DownloadFileTask().execute(mUrl);
        }

        // AsyncTask<Params,Progress,Result>
        private class DownloadFileTask extends AsyncTask<String, Void, String> {

            @Override
            protected String doInBackground(String... urls) {
                URL myFileUrl = null;
                try {
                    myFileUrl = new URL(urls[0]);
                } catch (MalformedURLException e) {
                    System.out.println("예외발생");
                }
                try {
                    HttpURLConnection conn = (HttpURLConnection) myFileUrl.openConnection();
                    conn.setDoInput(true);
                    conn.connect();
                    InputStream is = conn.getInputStream();

                    // 다운 받는 파일의 경로는 sdcard/ 에 저장되며 sdcard에 접근하려면 uses-permission에
                    // android.permission.WRITE_EXTERNAL_STORAGE을 추가해야만 가능.
                    String mPath = "sdcard/v3mobile.apk";
                    FileOutputStream fos;
                    File f = new File(mPath);
                    if (f.createNewFile()) {
                        fos = new FileOutputStream(mPath);
                        int read;
                        while ((read = is.read()) != -1) {
                            fos.write(read);
                        }
                        fos.close();
                    }

                    return "v3mobile.apk";
                } catch (IOException e) {
                    System.out.println("예외발생");
                    return "";
                }
            }

            @Override
            protected void onPostExecute(String filename) {
                if (!"".equals(filename)) {
                    Toast.makeText(getApplicationContext(), "download complete", Toast.LENGTH_SHORT).show();

                    // 안드로이드 패키지 매니저를 사용한 어플리케이션 설치.
                    File apkFile = new File(Environment.getExternalStorageDirectory() + "/" + filename);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
                    startActivity(intent);
                }
            }
        }


        // 외부 앱 호출
        public boolean callApp(String url) {
            Intent intent = null;
            // 인텐트 정합성 체크 : 2014 .01추가
            try {
                intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                setIntentSecurity(intent);
            } catch (URISyntaxException ex) {
                return false;
            }
            try {
                boolean retval = true;
                //chrome 버젼 방식 : 2014.01 추가
                if (url.startsWith("intent")) { // chrome 버젼 방식
                    // 앱설치 체크를 합니다.
                    if (getPackageManager().resolveActivity(intent, 0) == null) {
                        String packagename = intent.getPackage();
                        if (packagename != null) {
                            Uri uri = Uri.parse("market://search?q=pname:" + packagename);
                            intent = new Intent(Intent.ACTION_VIEW, uri);
                            setIntentSecurity(intent);
                            startActivity(intent);
                            retval = true;
                        }
                    } else {
                        intent.addCategory(Intent.CATEGORY_BROWSABLE);
                        intent.setComponent(null);
                        try {
                            if (startActivityIfNeeded(intent, -1)) {
                                retval = true;
                            }
                        } catch (ActivityNotFoundException ex) {
                            retval = false;
                        }
                    }
                } else { // 구 방식
                    Uri uri = Uri.parse(url);
                    intent = new Intent(Intent.ACTION_VIEW, uri);
                    setIntentSecurity(intent);
                    startActivity(intent);
                    retval = true;
                }
                return retval;
            } catch (ActivityNotFoundException e) {
                System.out.println("예외발생");
                return false;
            }
        }

        private void setIntentSecurity(Intent intent) {
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            // forbid explicit call
            intent.setComponent(null);
            // forbid Intent with selector Intent
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                intent.setSelector(null);
            }
        }
    }

    // App 체크 메소드 // 존재:true, 존재하지않음:false
    public static boolean isPackageInstalled(Context ctx, String pkgName) {
        try {
            ctx.getPackageManager().getPackageInfo(pkgName, PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            System.out.println("예외발생");
            return false;
        }
        return true;
    }

    public void showAlert(String message, String positiveButton, DialogInterface.OnClickListener positiveListener, String negativeButton, DialogInterface.OnClickListener negativeListener) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage(message);
        alert.setPositiveButton(positiveButton, positiveListener);
        alert.setNegativeButton(negativeButton, negativeListener);
        alert.show();
    }

    private class LguplusWebChromeClient extends WebChromeClient {

        public boolean onJsAlert(WebView view, String url, String message, final android.webkit.JsResult result) {
            new AlertDialog.Builder(PayActivity.this).setTitle("").setMessage(message).setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    result.confirm();
                }
            }).setCancelable(false).create().show();
            return true;
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
            new AlertDialog.Builder(PayActivity.this).setTitle("").setMessage(message).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    result.confirm();
                }
            }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    result.cancel();
                }
            }).create().show();
            return true;
        }
    }
}