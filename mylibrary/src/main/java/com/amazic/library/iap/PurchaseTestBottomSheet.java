package com.amazic.library.iap;

import android.content.Context;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;

import com.amazic.mylibrary.R;
import com.android.billingclient.api.ProductDetails;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class PurchaseTestBottomSheet extends BottomSheetDialog {
    private ProductDetails productDetails;
    private String typeIap;
    private TextView txtTitle;
    private TextView txtDescription;
    private TextView txtId;
    private TextView txtPrice;
    private TextView txtContinuePurchase;
    private PurchaseCallback purchaseCallback;

    public PurchaseTestBottomSheet(String typeIap, ProductDetails productDetails, @NonNull Context context, PurchaseCallback purchaseCallback) {
        super(context);
        this.productDetails = productDetails;
        this.typeIap = typeIap;
        this.purchaseCallback = purchaseCallback;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_billing_test);
        txtTitle = findViewById(R.id.txtTitle);
        txtDescription = findViewById(R.id.txtDescription);
        txtId = findViewById(R.id.txtId);
        txtPrice = findViewById(R.id.txtPrice);
        txtContinuePurchase = findViewById(R.id.txtContinuePurchase);
        if (productDetails == null) {

        } else {
            txtTitle.setText(productDetails.getTitle());
            txtDescription.setText(productDetails.getDescription());
            txtId.setText(productDetails.getProductId());
            if (typeIap.equals(IAPManager.typeIAP))
                txtPrice.setText(productDetails.getOneTimePurchaseOfferDetails().getFormattedPrice());
            else
                txtPrice.setText(productDetails.getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList().get(0).getFormattedPrice());

            txtContinuePurchase.setOnClickListener(v -> {
                if (purchaseCallback != null) {
                    IAPManager.getInstance().setPurchase(true);
                    purchaseCallback.onProductPurchased(productDetails.getProductId(), "{\"productId\":\"android.test.purchased\",\"type\":\"inapp\",\"title\":\"Tiêu đề mẫu\",\"description\":\"Mô tả mẫu về sản phẩm: android.test.purchased.\",\"skuDetailsToken\":\"AEuhp4Izz50wTvd7YM9wWjPLp8hZY7jRPhBEcM9GAbTYSdUM_v2QX85e8UYklstgqaRC\",\"oneTimePurchaseOfferDetails\":{\"priceAmountMicros\":23207002450,\"priceCurrencyCode\":\"VND\",\"formattedPrice\":\"23.207 ₫\"}}', parsedJson={\"productId\":\"android.test.purchased\",\"type\":\"inapp\",\"title\":\"Tiêu đề mẫu\",\"description\":\"Mô tả mẫu về sản phẩm: android.test.purchased.\",\"skuDetailsToken\":\"AEuhp4Izz50wTvd7YM9wWjPLp8hZY7jRPhBEcM9GAbTYSdUM_v2QX85e8UYklstgqaRC\",\"oneTimePurchaseOfferDetails\":{\"priceAmountMicros\":23207002450,\"priceCurrencyCode\":\"VND\",\"formattedPrice\":\"23.207 ₫\"}}, productId='android.test.purchased', productType='inapp', title='Tiêu đề mẫu', productDetailsToken='AEuhp4Izz50wTvd7YM9wWjPLp8hZY7jRPhBEcM9GAbTYSdUM_v2QX85e8UYklstgqaRC', subscriptionOfferDetails=null}");
                }
                dismiss();
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        int w = ViewGroup.LayoutParams.MATCH_PARENT;
        int h = ViewGroup.LayoutParams.WRAP_CONTENT;
        getWindow().setLayout(w, h);
    }
}
