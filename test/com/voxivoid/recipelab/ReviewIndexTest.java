package com.voxivoid.recipelab;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class ReviewIndexTest {
    @TempDir Path temp;
    private static final ReviewIndex.Cancel GO = new ReviewIndex.Cancel() { public boolean stopped() { return false; } };
    private File photo(String path, long time) throws Exception {
        Path p=temp.resolve("DCIM").resolve(path); Files.createDirectories(p.getParent()); Files.write(p,new byte[]{1});
        assertTrue(p.toFile().setLastModified(time)); return p.toFile();
    }
    @Test void noCardOrDcimFailsWithoutCreatingAnything() { assertThrows(IOException.class,()->ReviewIndex.scan(null,GO)); assertThrows(IOException.class,()->ReviewIndex.scan(temp.toFile(),GO)); assertFalse(Files.exists(temp.resolve("DCIM"))); }
    @Test void newestFirstAndCaseInsensitiveJpeg() throws Exception { photo("100MSDCF/DSC00001.JPG",10000); photo("100MSDCF/DSC00002.jpeg",20000); assertEquals("DSC00002.jpeg",ReviewIndex.scan(temp.toFile(),GO).get(0).file.getName()); }
    @Test void excludesRawVideoEmptyAndNestedFolders() throws Exception { photo("100MSDCF/DSC00001.ARW",10000); photo("100MSDCF/VID.MP4",10000); File empty=photo("100MSDCF/EMPTY.JPG",10000); Files.write(empty.toPath(),new byte[0]); photo("100MSDCF/sub/DEEP.JPG",10000); assertTrue(ReviewIndex.scan(temp.toFile(),GO).isEmpty()); }
    @Test void supportsDcimRootAndMultipleCameraFolders() throws Exception { photo("ROOT.JPG",10000); photo("100MSDCF/A.JPG",10000); photo("101MSDCF/B.JPG",10000); assertEquals(3,ReviewIndex.scan(temp.toFile(),GO).size()); }
    @Test void boundedNewestFiveHundred() throws Exception { for(int i=0;i<510;i++) photo("100MSDCF/P"+i+".JPG",10000+i*1000L); List<ReviewIndex.Photo> list=ReviewIndex.scan(temp.toFile(),GO); assertEquals(500,list.size()); assertEquals("P509.JPG",list.get(0).file.getName()); assertEquals("P10.JPG",list.get(499).file.getName()); }
    @Test void scanIsReadOnly() throws Exception { File f=photo("100MSDCF/A.JPG",1234000); byte[] before=Files.readAllBytes(f.toPath()); long m=f.lastModified(); ReviewIndex.scan(temp.toFile(),GO); assertArrayEquals(before,Files.readAllBytes(f.toPath())); assertEquals(m,f.lastModified()); assertEquals(1,Objects.requireNonNull(f.getParentFile().list()).length); }
    @Test void cancelledScanPublishesNoPartialList() throws Exception { photo("100MSDCF/A.JPG",10000); assertTrue(ReviewIndex.scan(temp.toFile(),new ReviewIndex.Cancel(){public boolean stopped(){return true;}}).isEmpty()); }
    @Test void corruptDimensionsRejected() { for(int[] d:new int[][]{{0,20},{20,0},{-1,20},{25000,20},{20000,20000}}) assertThrows(IOException.class,()->ReviewIndex.sample(d[0],d[1])); }
    @Test void twentyMegapixelImageIsSubsampled() throws Exception { int n=ReviewIndex.sample(5472,3648); assertTrue((5472+n-1)/n<=800); assertTrue((3648+n-1)/n<=800); assertEquals(8,n); }
    @Test void smallJpegNotUpsampledByDecoder() throws Exception { assertEquals(1,ReviewIndex.sample(640,480)); }
    @Test void pagingWrapsAndEmptyHasNoSelection() { assertEquals(-1,ReviewIndex.next(0,1,0)); assertEquals(0,ReviewIndex.next(0,1,1)); assertEquals(2,ReviewIndex.next(0,-1,3)); assertEquals(0,ReviewIndex.next(2,1,3)); }
    @Test void sonyContractDoesNotPullBackPlaybackOrShutterToNativeUi() { assertArrayEquals(new String[]{"KEY_USB_CONNECT"},SonyAppSession.pullingBackKeys()); List<String> keys=Arrays.asList(SonyAppSession.resumeKeys()); assertTrue(keys.contains("KEY_PLAY_APO")); assertTrue(keys.contains("KEY_PLAY_PON")); assertTrue(keys.contains("KEY_RELEASE_APO")); assertEquals(9,new HashSet<String>(keys).size()); }
    @Test void contractArraysCannotBeMutatedByCallers() { SonyAppSession.pullingBackKeys()[0]="KEY_PLAY"; assertEquals("KEY_USB_CONNECT",SonyAppSession.pullingBackKeys()[0]); }
}
