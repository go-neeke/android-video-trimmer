package com.gowtham.library.utils;

import android.os.Parcel;
import android.os.Parcelable;

public class TrimVideoOptions implements Parcelable {

    public String fileName;

    public TrimType trimType;

    public long minDuration, fixedDuration;

    public boolean hideSeekBar;

    public boolean accurateCut;

    public boolean showFileLocationAlert;

    public long[] minToMax;

    public String title;

    public String local;

    public CompressOption compressOption;

    public boolean isEnableEdit;

    public boolean isExecute;

    public TrimVideoOptions() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.fileName);
        dest.writeInt(this.trimType == null ? -1 : this.trimType.ordinal());
        dest.writeLong(this.minDuration);
        dest.writeLong(this.fixedDuration);
        dest.writeByte(this.hideSeekBar ? (byte) 1 : (byte) 0);
        dest.writeByte(this.accurateCut ? (byte) 1 : (byte) 0);
        dest.writeByte(this.showFileLocationAlert ? (byte) 1 : (byte) 0);
        dest.writeLongArray(this.minToMax);
        dest.writeString(this.title);
        dest.writeString(this.local);
        dest.writeByte(this.isEnableEdit ? (byte) 1 : (byte) 0);
        dest.writeByte(this.isExecute ? (byte) 1 : (byte) 0);
        dest.writeParcelable((Parcelable) this.compressOption, flags);
    }

    protected TrimVideoOptions(Parcel in) {
        this.fileName = in.readString();
        int tmpTrimType = in.readInt();
        this.trimType = tmpTrimType == -1 ? null : TrimType.values()[tmpTrimType];
        this.minDuration = in.readLong();
        this.fixedDuration = in.readLong();
        this.hideSeekBar = in.readByte() != 0;
        this.accurateCut = in.readByte() != 0;
        this.showFileLocationAlert = in.readByte() != 0;
        this.minToMax = in.createLongArray();
        this.title = in.readString();
        this.local = in.readString();
        this.isEnableEdit = in.readByte() != 0;
        this.isExecute = in.readByte() != 0;
        this.compressOption = in.readParcelable(CompressOption.class.getClassLoader());
    }

    public static final Creator<TrimVideoOptions> CREATOR = new Creator<TrimVideoOptions>() {
        @Override
        public TrimVideoOptions createFromParcel(Parcel source) {
            return new TrimVideoOptions(source);
        }

        @Override
        public TrimVideoOptions[] newArray(int size) {
            return new TrimVideoOptions[size];
        }
    };
}
