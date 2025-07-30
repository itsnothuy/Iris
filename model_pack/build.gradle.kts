plugins { id("com.android.asset-pack") }

assetPack {
    packName.set("model_pack")
    dynamicDelivery { deliveryType.set("install-time") }
}
