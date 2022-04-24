package com.faforever.neroxis.util.dds;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Data;

@Data
public strictfp class DDSHeader {

    final public static int CAPS_FLAG = 0x1;
    final public static int HEIGHT_FLAG = 0x2;
    final public static int WIDTH_FLAG = 0x4;
    final public static int PITCH_FLAG = 0x8;
    final public static int PIXELFORMAT_FLAG = 0x1000;
    final public static int MIPMAPCOUNT_FLAG = 0x20000;
    final public static int LINEARSIZE_FLAG = 0x80000;
    final public static int DEPTH_FLAG = 0x800000;
    final public static int ALPHAPIXELS_FLAG = 0x1;
    final public static int FOURCC_FLAG = 0x4;
    final public static int RGB_FLAG = 0x40;
    final public static int COMPLEX_FLAG = 0x8;
    final public static int TEXTURE_FLAG = 0x1000;
    final public static int MIPMAP_FLAG = 0x400000;
    final public static int CUBEMAP_FLAG = 0x200;
    final public static int CUBEMAP_POSITIVEX_FLAG = 0x400;
    final public static int CUBEMAP_NEGATIVEX_FLAG = 0x800;
    final public static int CUBEMAP_POSITIVEY_FLAG = 0x1000;
    final public static int CUBEMAP_NEGATIVEY_FLAG = 0x2000;
    final public static int CUBEMAP_POSITIVEZ_FLAG = 0x4000;
    final public static int CUBEMAP_NEGATIVEZ_FLAG = 0x8000;
    final public static int VOLUME_FLAG = 0x200000;
    final private String magic = "DDS ";
    final private int size = 124;
    final private int[] reserved1 = new int[11];
    final private int pixelFormatSize = 32;
    private ByteBuffer headerBytesBuffer;
    private int flags;
    private int height;
    private int width;
    private int pitchOrLinearSize;
    private int depth;
    private int mipMapCount;
    private int pixelFlags;
    private String fourCC;
    private int RGBBitCount;
    private int RBitMask;
    private int GBitMask;
    private int BBitMask;
    private int ABitMask;
    private int caps1;
    private int caps2;
    private int caps3 = 0;
    private int caps4 = 0;
    private int reserved2 = 0;

    public DDSHeader() {
        headerBytesBuffer = ByteBuffer.allocate(128).order(ByteOrder.LITTLE_ENDIAN);
        headerBytesBuffer.put(magic.getBytes(), 0, 4);
        headerBytesBuffer.putInt(4, size);
        for (int i = 0; i < reserved1.length; i++) {
            headerBytesBuffer.putInt(32 + i * 4, reserved1[i]);
        }
        headerBytesBuffer.putInt(19 * 4, pixelFormatSize);
        headerBytesBuffer.putInt(29 * 4, caps3);
        headerBytesBuffer.putInt(30 * 4, caps4);
        headerBytesBuffer.putInt(31 * 4, reserved2);
        setCapsFlag(true);
        setPixelFormatFlag(true);
        setWidthFlag(true);
        setHeightFlag(true);
        setTextureFlag(true);
    }

    public static DDSHeader parseHeader(byte[] bytes) {
        DDSHeader header = new DDSHeader();
        ByteBuffer headerBytesBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        byte[] magicBytes = new byte[4];
        headerBytesBuffer.get(magicBytes);
        if (!new String(magicBytes).equals(header.magic)) {
            throw new IllegalArgumentException("Not a valid DDS Header");
        }
        int inSize = headerBytesBuffer.getInt();
        if (inSize != header.size) {
            throw new IllegalArgumentException("Not a valid DDS Header");
        }
        header.setFlags(headerBytesBuffer.getInt());
        header.setHeight(headerBytesBuffer.getInt());
        header.setWidth(headerBytesBuffer.getInt());
        header.setPitchOrLinearSize(headerBytesBuffer.getInt());
        header.setDepth(headerBytesBuffer.getInt());
        header.setMipMapCount(headerBytesBuffer.getInt());
        for (int i = 0; i < header.reserved1.length; i++) {
            header.reserved1[i] = headerBytesBuffer.getInt();
        }
        int inPixelFormatSize = headerBytesBuffer.getInt();
        if (inPixelFormatSize != header.pixelFormatSize) {
            throw new IllegalArgumentException("Not a valid DDS Header");
        }
        header.setPixelFlags(headerBytesBuffer.getInt());
        byte[] fourCCBytes = new byte[4];
        headerBytesBuffer.get(fourCCBytes);
        header.setFourCC(new String(fourCCBytes));
        header.setRGBBitCount(headerBytesBuffer.getInt());
        header.setRBitMask(headerBytesBuffer.getInt());
        header.setGBitMask(headerBytesBuffer.getInt());
        header.setBBitMask(headerBytesBuffer.getInt());
        header.setABitMask(headerBytesBuffer.getInt());
        header.setCaps1(headerBytesBuffer.getInt());
        header.setCaps2(headerBytesBuffer.getInt());
        header.setCaps3(headerBytesBuffer.getInt());
        header.setCaps4(headerBytesBuffer.getInt());
        header.setReserved2(headerBytesBuffer.getInt());
        return header;
    }

    public boolean getCapsFlag() {
        return (flags & CAPS_FLAG) > 0;
    }

    public void setCapsFlag(boolean val) {
        if (val) {
            flags |= CAPS_FLAG;
        } else {
            flags &= ~CAPS_FLAG;
        }
        setFlags(flags);
    }

    public void setFlags(int flags) {
        this.flags = flags;
        headerBytesBuffer.putInt(2 * 4, flags);
    }

    public boolean getHeightFlag() {
        return (flags & HEIGHT_FLAG) > 0;
    }

    public void setHeightFlag(boolean val) {
        if (val) {
            flags |= HEIGHT_FLAG;
        } else {
            flags &= ~HEIGHT_FLAG;
        }
        setFlags(flags);
    }

    public boolean getWidthFlag() {
        return (flags & WIDTH_FLAG) > 0;
    }

    public void setWidthFlag(boolean val) {
        if (val) {
            flags |= WIDTH_FLAG;
        } else {
            flags &= ~WIDTH_FLAG;
        }
        setFlags(flags);
    }

    public boolean getPitchFlag() {
        return (flags & PITCH_FLAG) > 0;
    }

    public void setPitchFlag(boolean val) {
        if (val) {
            flags |= PITCH_FLAG;
        } else {
            flags &= ~PITCH_FLAG;
        }
        setFlags(flags);
    }

    public boolean getPixelFormatFlag() {
        return (flags & PIXELFORMAT_FLAG) > 0;
    }

    public void setPixelFormatFlag(boolean val) {
        if (val) {
            flags |= PIXELFORMAT_FLAG;
        } else {
            flags &= ~PIXELFORMAT_FLAG;
        }
        setFlags(flags);
    }

    public boolean getMipMapCountFlag() {
        return (flags & MIPMAPCOUNT_FLAG) > 0;
    }

    public void setMipMapCountFlag(boolean val) {
        if (val) {
            flags |= MIPMAPCOUNT_FLAG;
        } else {
            flags &= ~MIPMAPCOUNT_FLAG;
        }
        setFlags(flags);
    }

    public boolean getLinearSizeFlag() {
        return (flags & LINEARSIZE_FLAG) > 0;
    }

    public void setLinearSizeFlag(boolean val) {
        if (val) {
            flags |= LINEARSIZE_FLAG;
        } else {
            flags &= ~LINEARSIZE_FLAG;
        }
        setFlags(flags);
    }

    public boolean getDepthFlag() {
        return (flags & DEPTH_FLAG) > 0;
    }

    public void setDepthFlag(boolean val) {
        if (val) {
            flags |= DEPTH_FLAG;
        } else {
            flags &= ~DEPTH_FLAG;
        }
        setFlags(flags);
    }

    public boolean getAlphaPixelsFlag() {
        return (pixelFlags & ALPHAPIXELS_FLAG) > 0;
    }

    public void setAlphaPixelsFlag(boolean val) {
        if (val) {
            pixelFlags |= ALPHAPIXELS_FLAG;
        } else {
            pixelFlags &= ~ALPHAPIXELS_FLAG;
        }
        setPixelFlags(pixelFlags);
    }

    public boolean getFourCCFlag() {
        return (pixelFlags & FOURCC_FLAG) > 0;
    }

    public void setFourCCFlag(boolean val) {
        if (val) {
            pixelFlags |= FOURCC_FLAG;
        } else {
            pixelFlags &= ~FOURCC_FLAG;
        }
        setPixelFlags(pixelFlags);
    }

    public boolean getRGBFlag() {
        return (pixelFlags & RGB_FLAG) > 0;
    }

    public void setRGBFlag(boolean val) {
        if (val) {
            pixelFlags |= RGB_FLAG;
        } else {
            pixelFlags &= ~RGB_FLAG;
        }
        setPixelFlags(pixelFlags);
    }

    public boolean getComplexFlag() {
        return (caps1 & COMPLEX_FLAG) > 0;
    }

    public void setComplexFlag(boolean val) {
        if (val) {
            caps1 |= COMPLEX_FLAG;
        } else {
            caps1 &= ~COMPLEX_FLAG;
        }
        setCaps1(caps1);
    }

    public boolean getTextureFlag() {
        return (caps1 & TEXTURE_FLAG) > 0;
    }

    public void setTextureFlag(boolean val) {
        if (val) {
            caps1 |= TEXTURE_FLAG;
        } else {
            caps1 &= ~TEXTURE_FLAG;
        }
        setCaps1(caps1);
    }

    public void setCaps1(int caps1) {
        this.caps1 = caps1;
        headerBytesBuffer.putInt(27 * 4, caps1);
    }

    public boolean getMipMapFlag() {
        return (caps1 & MIPMAP_FLAG) > 0;
    }

    public void setMipMapFlag(boolean val) {
        if (val) {
            caps1 |= MIPMAP_FLAG;
        } else {
            caps1 &= ~MIPMAP_FLAG;
        }
        setCaps1(caps1);
    }

    public boolean getCubeMapFlag() {
        return (caps2 & CUBEMAP_FLAG) > 0;
    }

    public void setCubeMapFlag(boolean val) {
        if (val) {
            caps2 |= CUBEMAP_FLAG;
        } else {
            caps2 &= ~CUBEMAP_FLAG;
        }
        setCaps2(caps2);
    }

    public void setCaps2(int caps2) {
        this.caps2 = caps2;
        headerBytesBuffer.putInt(28 * 4, caps2);
    }

    public boolean getCubeMapPositiveXFlag() {
        return (pixelFlags & CUBEMAP_POSITIVEX_FLAG) > 0;
    }

    public void setCubeMapPositiveXFlag(boolean val) {
        if (val) {
            caps2 |= CUBEMAP_POSITIVEX_FLAG;
        } else {
            caps2 &= ~CUBEMAP_POSITIVEX_FLAG;
        }
        setCaps2(caps2);
    }

    public boolean getCubeMapNegativeXFlag() {
        return (pixelFlags & CUBEMAP_NEGATIVEX_FLAG) > 0;
    }

    public void setCubeMapNegativeXFlag(boolean val) {
        if (val) {
            caps2 |= CUBEMAP_NEGATIVEX_FLAG;
        } else {
            caps2 &= ~CUBEMAP_NEGATIVEX_FLAG;
        }
        setCaps2(caps2);
    }

    public boolean getCubeMapPositiveYFlag() {
        return (pixelFlags & CUBEMAP_POSITIVEY_FLAG) > 0;
    }

    public void setCubeMapPositiveYFlag(boolean val) {
        if (val) {
            caps2 |= CUBEMAP_POSITIVEY_FLAG;
        } else {
            caps2 &= ~CUBEMAP_POSITIVEY_FLAG;
        }
        setCaps2(caps2);
    }

    public boolean getCubeMapNegativeYFlag() {
        return (pixelFlags & CUBEMAP_NEGATIVEY_FLAG) > 0;
    }

    public void setCubeMapNegativeYFlag(boolean val) {
        if (val) {
            caps2 |= CUBEMAP_NEGATIVEY_FLAG;
        } else {
            caps2 &= ~CUBEMAP_NEGATIVEY_FLAG;
        }
        setCaps2(caps2);
    }

    public boolean getCubeMapPositiveZFlag() {
        return (pixelFlags & CUBEMAP_POSITIVEZ_FLAG) > 0;
    }

    public void setCubeMapPositiveZFlag(boolean val) {
        if (val) {
            caps2 |= CUBEMAP_POSITIVEZ_FLAG;
        } else {
            caps2 &= ~CUBEMAP_POSITIVEZ_FLAG;
        }
        setCaps2(caps2);
    }

    public boolean getCubeMapNegativeZFlag() {
        return (pixelFlags & CUBEMAP_NEGATIVEZ_FLAG) > 0;
    }

    public void setCubeMapNegativeZFlag(boolean val) {
        if (val) {
            caps2 |= CUBEMAP_NEGATIVEZ_FLAG;
        } else {
            caps2 &= ~CUBEMAP_NEGATIVEZ_FLAG;
        }
        setCaps2(caps2);
    }

    public boolean getVolumeFlag() {
        return (pixelFlags & VOLUME_FLAG) > 0;
    }

    public void setVolumeFlag(boolean val) {
        if (val) {
            caps2 |= VOLUME_FLAG;
        } else {
            caps2 &= ~VOLUME_FLAG;
        }
        setCaps2(caps2);
    }

    public void setHeight(int height) {
        this.height = height;
        headerBytesBuffer.putInt(3 * 4, height);
    }

    public void setWidth(int width) {
        this.width = width;
        headerBytesBuffer.putInt(4 * 4, width);
    }

    public void setPitchOrLinearSize(int pitchOrLinearSize, boolean compressed) {
        this.pitchOrLinearSize = pitchOrLinearSize;
        headerBytesBuffer.putInt(5 * 4, pitchOrLinearSize);
        if (pitchOrLinearSize != 0 && compressed) {
            setLinearSizeFlag(true);
            setPitchFlag(false);
        } else if (pitchOrLinearSize != 0) {
            setPitchFlag(true);
            setLinearSizeFlag(false);
        } else {
            setPitchFlag(false);
            setLinearSizeFlag(false);
        }
    }

    public void setDepth(int depth) {
        this.depth = depth;
        headerBytesBuffer.putInt(6 * 4, depth);
        setDepthFlag(depth != 0);
    }

    public void setMipMapCount(int mipMapCount) {
        this.mipMapCount = mipMapCount;
        headerBytesBuffer.putInt(7 * 4, mipMapCount);
        if (mipMapCount != 0) {
            setMipMapCountFlag(true);
            setMipMapFlag(true);
        } else {
            setMipMapCountFlag(false);
            setMipMapFlag(false);
        }
    }

    public void setPixelFlags(int pixelFlags) {
        this.pixelFlags = pixelFlags;
        headerBytesBuffer.putInt(20 * 4, pixelFlags);
    }

    public void setFourCC(String fourCC) {
        this.fourCC = fourCC;
        ByteBuffer fourCCByteBuffer = ByteBuffer.allocate(4);
        fourCCByteBuffer.put(fourCC.getBytes());
        byte[] fourCCbytes = fourCCByteBuffer.array();
        for (int i = 0; i < 4; i++) {
            headerBytesBuffer.put(21 * 4 + i, fourCCbytes[i]);
        }
        setFourCCFlag(fourCC.length() == 4);
        setRGBFlag(fourCC.length() != 4);
    }

    public void setRGBBitCount(int RGBBitCount) {
        this.RGBBitCount = RGBBitCount;
        headerBytesBuffer.putInt(22 * 4, RGBBitCount);
        setRGBFlag(RGBBitCount != 0);
        setFourCCFlag(RGBBitCount == 0);
    }

    public void setRBitMask(int RBitMask) {
        this.RBitMask = RBitMask;
        headerBytesBuffer.putInt(23 * 4, RBitMask);
    }

    public void setGBitMask(int GBitMask) {
        this.GBitMask = GBitMask;
        headerBytesBuffer.putInt(24 * 4, GBitMask);
    }

    public void setBBitMask(int BBitMask) {
        this.BBitMask = BBitMask;
        headerBytesBuffer.putInt(25 * 4, BBitMask);
    }

    public void setABitMask(int ABitMask) {
        this.ABitMask = ABitMask;
        headerBytesBuffer.putInt(26 * 4, ABitMask);
        setAlphaPixelsFlag(ABitMask != 0);
    }

    public void setCaps3(int caps3) {
        this.caps3 = caps3;
        headerBytesBuffer.putInt(29 * 4, caps3);
    }

    public void setCaps4(int caps4) {
        this.caps4 = caps4;
        headerBytesBuffer.putInt(30 * 4, caps4);
    }

    public byte[] toBytes() {
        return headerBytesBuffer.array();
    }
}
