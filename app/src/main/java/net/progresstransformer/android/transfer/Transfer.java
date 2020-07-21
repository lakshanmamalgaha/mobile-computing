package net.progresstransformer.android.transfer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import net.progresstransformer.android.bundle.Bundle;
import net.progresstransformer.android.bundle.FileItem;
import net.progresstransformer.android.bundle.Item;
import net.progresstransformer.android.bundle.UrlItem;
import net.progresstransformer.android.discovery.Device;
import net.progresstransformer.android.viewData.Database.DBHelper;
import net.progresstransformer.android.viewData.MainActivity;
import net.progresstransformer.android.viewData.PlayVideo.PlayerVideo;
import net.progresstransformer.android.viewData.VideoLoder.Constant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Perform a transfer from one device to another
 * <p>
 * This class takes care of communicating (via socket) with another device to
 * transfer a bundle (list of items) using packets.
 */
public class Transfer implements Runnable {

    private static final int CHUNK_SIZE = 65536;
    private static final Gson mGson = new Gson();
    public DBHelper dbHelper;
    public Context mcontext;


    /**
     * Listener for status changes
     */
    interface StatusChangedListener {
        void onStatusChanged(TransferStatus transferStatus);
    }

    /**
     * Listener for item received events
     */
    interface ItemReceivedListener {
        void onItemReceived(Item item);
    }

    /**
     * Transfer header
     */
    private class TransferHeader {
        String name;
        String count;
        String size;
    }

    // Internal state of the transfer
    private enum InternalState {
        TransferHeader,
        ItemHeader,
        ItemContent,
        Finished,
    }

    private final TransferStatus mTransferStatus;
    private volatile boolean mStop = false;

    private final List<StatusChangedListener> mStatusChangedListeners = new ArrayList<>();
    private final List<ItemReceivedListener> mItemReceivedListeners = new ArrayList<>();

    private Device mDevice;
    private Bundle mBundle;
    private String mDeviceName;
    private String mTransferDirectory;
    private boolean mOverwrite;

    private SocketChannel mSocketChannel;
    private Selector mSelector = Selector.open();

    private InternalState mInternalState = InternalState.TransferHeader;

    private Packet mReceivingPacket;
    private Packet mSendingPacket;

    private int mTransferItems;
    private long mTransferBytesTotal;
    private long mTransferBytesTransferred;

    private Item mItem;
    private int mItemIndex;
    private long mItemBytesRemaining;

    /**
     * Create a transfer for receiving items
     *
     * @param socketChannel     incoming channel
     * @param transferDirectory directory for incoming files
     * @param overwrite         true to overwrite existing files
     * @param unknownDeviceName device name shown before being received
     */
    public Transfer(Context context,SocketChannel socketChannel, String transferDirectory, boolean overwrite, String unknownDeviceName) throws IOException {
        mTransferStatus = new TransferStatus(unknownDeviceName,
                TransferStatus.Direction.Receive, TransferStatus.State.Transferring);
        mTransferDirectory = transferDirectory;
        mOverwrite = overwrite;
        mSocketChannel = socketChannel;
        mSocketChannel.configureBlocking(false);
        mcontext=context;
    }

    /**
     * Create a transfer for sending items
     *
     * @param device     device to connect to
     * @param deviceName device name to send to the remote device
     * @param bundle     bundle to transfer
     */
    public Transfer(Device device, String deviceName, Bundle bundle) throws IOException {
        mTransferStatus = new TransferStatus(device.getName(),
                TransferStatus.Direction.Send, TransferStatus.State.Connecting);
        mDevice = device;
        mBundle = bundle;
        mDeviceName = deviceName;
        mSocketChannel = SocketChannel.open();
        mSocketChannel.configureBlocking(false);
        mTransferItems = bundle.size();
        mTransferBytesTotal = bundle.getTotalSize();
        mTransferStatus.setBytesTotal(mTransferBytesTotal);
    }

    /**
     * Set the transfer ID
     */
    public void setId(int id) {
        synchronized (mTransferStatus) {
            mTransferStatus.setId(id);
        }
    }

    /**
     * Retrieve the current transfer status
     *
     * @return copy of the current status
     */
    public TransferStatus getStatus() {
        synchronized (mTransferStatus) {
            return new TransferStatus(mTransferStatus);
        }
    }

    /**
     * Close the socket and wake the selector, effectively aborting the transfer
     */
    void stop() {
        mStop = true;
        mSelector.wakeup();
    }

    /**
     * Add a listener for status changes
     * <p>
     * This method should not be invoked after starting the transfer.
     */
    void addStatusChangedListener(StatusChangedListener statusChangedListener) {
        mStatusChangedListeners.add(statusChangedListener);
    }

    /**
     * Add a listener for items being recieved
     * <p>
     * This method should not be invoked after starting the transfer.
     */
    void addItemReceivedListener(ItemReceivedListener itemReceivedListener) {
        mItemReceivedListeners.add(itemReceivedListener);
    }

    /**
     * Notify all listeners that the status has changed
     */
    private void notifyStatusChangedListeners() {
        for (StatusChangedListener statusChangedListener : mStatusChangedListeners) {
            statusChangedListener.onStatusChanged(new TransferStatus(mTransferStatus));
        }
    }

    /**
     * Update current transfer progress
     */
    private void updateProgress() {
        int newProgress = (int) (100.0 * (mTransferBytesTotal != 0 ?
                (double) mTransferBytesTransferred / (double) mTransferBytesTotal : 0.0));
        if (newProgress != mTransferStatus.getProgress()) {
            synchronized (mTransferStatus) {
                mTransferStatus.setProgress(newProgress);
                mTransferStatus.setBytesTransferred(mTransferBytesTransferred);
                notifyStatusChangedListeners();
            }
        }
    }

    /**
     * Process the transfer header
     */
    private void processTransferHeader() throws IOException {
        TransferHeader transferHeader;
        try {
            transferHeader = mGson.fromJson(new String(
                            mReceivingPacket.getBuffer().array(), Charset.forName("UTF-8")),
                    TransferHeader.class);
            mTransferItems = Integer.parseInt(transferHeader.count);
            mTransferBytesTotal = Long.parseLong(transferHeader.size);
        } catch (JsonSyntaxException | NumberFormatException e) {
            throw new IOException(e.getMessage());
        }
        mInternalState = mItemIndex == mTransferItems ? InternalState.Finished : InternalState.ItemHeader;
        synchronized (mTransferStatus) {
            mTransferStatus.setRemoteDeviceName(transferHeader.name);
            mTransferStatus.setBytesTotal(mTransferBytesTotal);
            notifyStatusChangedListeners();
        }
    }

    /**
     * Process the header for an individual item
     */
    private void processItemHeader() throws IOException {
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        Map<String, Object> map;
        try {
            map = mGson.fromJson(new String(
                    mReceivingPacket.getBuffer().array(), Charset.forName("UTF-8")), type);
        } catch (JsonSyntaxException e) {
            throw new IOException(e.getMessage());
        }
        String itemType = (String) map.get(Item.TYPE);
        if (itemType == null) {
            itemType = FileItem.TYPE_NAME;
        }
        switch (itemType) {
            case FileItem.TYPE_NAME:
                mItem = new FileItem(mTransferDirectory, map, mOverwrite);

                break;
            case UrlItem.TYPE_NAME:
                mItem = new UrlItem(map);
                break;
            default:
                throw new IOException("unrecognized item type");
        }
        long itemSize = mItem.getLongProperty(Item.SIZE, true);
        if (itemSize != 0) {
            mInternalState = InternalState.ItemContent;
            mItem.open(Item.Mode.Write);
            mItemBytesRemaining = itemSize;
        } else {
            processNext();
        }
    }


    /**
     * Process item contents
     */
    private void processItemContent() throws IOException {
        mItem.write(mReceivingPacket.getBuffer().array());
        int numBytes = mReceivingPacket.getBuffer().capacity();
        mTransferBytesTransferred += numBytes;
        mItemBytesRemaining -= numBytes;
        updateProgress();
        if (mItemBytesRemaining <= 0) {
            mItem.close();
            processNext();
        }
    }

    /**
     * Prepare to process the next item
     */
    private void processNext() {
        mItemIndex += 1;
        mInternalState = mItemIndex == mTransferItems ? InternalState.Finished : InternalState.ItemHeader;
        for (ItemReceivedListener itemReceivedListener : mItemReceivedListeners) {
            itemReceivedListener.onItemReceived(mItem);
        }
    }

    /**
     * Process the next packet by reading it and then invoking the correct method
     *
     * @return true if there are more packets expected
     */
    private boolean processNextPacket() throws IOException {
        if (mReceivingPacket == null) {
            mReceivingPacket = new Packet();
        }
        mReceivingPacket.read(mSocketChannel);
        if (mReceivingPacket.isFull()) {
            if (mReceivingPacket.getType() == Packet.ERROR) {
                throw new IOException(new String(mReceivingPacket.getBuffer().array(),
                        Charset.forName("UTF-8")));
            }
            if (mTransferStatus.getDirection() == TransferStatus.Direction.Receive) {
                if (mInternalState == InternalState.TransferHeader && mReceivingPacket.getType() == Packet.JSON) {
                    processTransferHeader();
                } else if (mInternalState == InternalState.ItemHeader && mReceivingPacket.getType() == Packet.JSON) {
                    processItemHeader();
                } else if (mInternalState == InternalState.ItemContent && mReceivingPacket.getType() == Packet.BINARY) {
                    processItemContent();
                } else {
                    throw new IOException("unexpected packet");
                }
                mReceivingPacket = null;
                return mInternalState != InternalState.Finished;
            } else {
                if (mInternalState == InternalState.Finished && mReceivingPacket.getType() == Packet.SUCCESS) {
                    return false;
                } else {
                    throw new IOException("unexpected packet");
                }
            }
        } else {
            return true;
        }
    }

    /**
     * Send the transfer header
     */
    private void sendTransferHeader() {
        Map<String, String> map = new HashMap<>();
        map.put("name", mDeviceName);
        map.put("count", Integer.toString(mBundle.size()));
        map.put("size", Long.toString(mBundle.getTotalSize()));
        mSendingPacket = new Packet(Packet.JSON, mGson.toJson(map).getBytes(
                Charset.forName("UTF-8")));
        mInternalState = mItemIndex == mTransferItems ? InternalState.Finished : InternalState.ItemHeader;
    }

    /**
     * Send the header for an individual item
     */
    private void sendItemHeader() throws IOException {
        mItem = mBundle.get(mItemIndex);
        mSendingPacket = new Packet(Packet.JSON, mGson.toJson(
                mItem.getProperties()).getBytes(Charset.forName("UTF-8")));
        long itemSize = mItem.getLongProperty(Item.SIZE, true);
        if (itemSize != 0) {
            mInternalState = InternalState.ItemContent;
            mItem.open(Item.Mode.Read);
            mItemBytesRemaining = itemSize;
        } else {
            mItemIndex += 1;
            mInternalState = mItemIndex == mTransferItems ? InternalState.Finished : InternalState.ItemHeader;
        }
    }

    /**
     * Send item contents
     */
    private void sendItemContent() throws IOException {
        byte buffer[] = new byte[CHUNK_SIZE];
        int numBytes = mItem.read(buffer);
        mSendingPacket = new Packet(Packet.BINARY, buffer, numBytes);
        mTransferBytesTransferred += numBytes;
        mItemBytesRemaining -= numBytes;
        updateProgress();
        if (mItemBytesRemaining <= 0) {
            mItem.close();
            mItemIndex += 1;
            mInternalState = mItemIndex == mTransferItems ? InternalState.Finished : InternalState.ItemHeader;
        }
    }

    /**
     * Send the next packet by evaluating the current state
     *
     * @return true if there are more packets to send
     */
    private boolean sendNextPacket() throws IOException {
        if (mSendingPacket == null) {
            if (mTransferStatus.getDirection() == TransferStatus.Direction.Receive) {
                mSendingPacket = new Packet(Packet.SUCCESS);
            } else {
                switch (mInternalState) {
                    case TransferHeader:
                        sendTransferHeader();
                        break;
                    case ItemHeader:
                        sendItemHeader();
                        break;
                    case ItemContent:
                        sendItemContent();
                        break;
                    default:
                        throw new IOException("unreachable code");
                }
            }
        }
        mSocketChannel.write(mSendingPacket.getBuffer());
        if (mSendingPacket.isFull()) {
            mSendingPacket = null;
            return mInternalState != InternalState.Finished;
        }
        return true;
    }

    /**
     * Perform the transfer until it completes or an error occurs
     */
    public String getdata;
    public String filename;
    public String progressReceived;
    public String typeReceived;
    public String fileNamereceived;

    @Override
    public void run() {

        dbHelper=new DBHelper(mcontext);
        try {
            Log.d("aa","652545565");
            // Indicate which operations select() should select for
            SelectionKey selectionKey = mSocketChannel.register(
                    mSelector,
                    mTransferStatus.getDirection() == TransferStatus.Direction.Receive ?
                            SelectionKey.OP_READ :
                            SelectionKey.OP_CONNECT
            );

            // For a sending transfer, connect to the remote device
            if (mTransferStatus.getDirection() == TransferStatus.Direction.Send) {
                mSocketChannel.connect(new InetSocketAddress(mDevice.getHost(), mDevice.getPort()));
            }

            while (true) {
                mSelector.select();
                if (mStop) {
                    break;
                }
                if (selectionKey.isConnectable()) {
                    mSocketChannel.finishConnect();
                    selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);

                    synchronized (mTransferStatus) {
                        mTransferStatus.setState(TransferStatus.State.Transferring);
                        notifyStatusChangedListeners();
                    }
                }
                if (selectionKey.isReadable()) {
                    if (!processNextPacket()) {
                        if (mTransferStatus.getDirection() == TransferStatus.Direction.Receive) {
                            selectionKey.interestOps(SelectionKey.OP_WRITE);
                        } else {
                            break;
                        }
                    }
                }
                if (selectionKey.isWritable()) {
                    if (!sendNextPacket()) {
                        if (mTransferStatus.getDirection() == TransferStatus.Direction.Receive) {

                            getdata=readFile();
                            String[] getdataList=getdata.split(" ");

                            for(int i=0; i<getdataList.length; i++){
                                if (i==0){
                                    progressReceived=getdataList[0];
                                }else if(i==getdataList.length-1){
                                    typeReceived=getdataList[getdataList.length-1];
                                }else{
                                    if (i==1){
                                        fileNamereceived=getdataList[1];
                                    }else {
                                        fileNamereceived += getdataList[i];
                                        }
                                    }
                            }
                            File storage = Environment.getExternalStorageDirectory();
                            File downloads = new File(storage, "Download");
                            String nitroShare1=new File(downloads, "NitroShare").getAbsolutePath();
                            //String uri=nitroShare1+"/"+fileNamereceived;
                            String uri="file:///storage/emulated/0/Download/NitroShare/"+fileNamereceived;
                            Uri uri1= Uri.parse(uri);
                            if (typeReceived.equals("video\n")){
                                Log.d("aaa",progressReceived);



                                ArrayList<HashMap<String, String>> progressAttay1 = dbHelper.getvideoData(String.valueOf(uri1));
                                if (progressAttay1.isEmpty()) {
                                    dbHelper.insertData(fileNamereceived, uri, progressReceived);
                                    Log.d("aaa","add neww");
                                    //Log.d("aa",progressReceived);
                                    //Constant.allSendToDB.add(String.valueOf(urlOfVideo));
                                } else {
                                    dbHelper.updateProgress(progressReceived, uri);
                                    Log.d("aaa","update neww");
                                }
                                Log.d("aaa","sfkhksdgldsgl"+dbHelper.getvideoData(String.valueOf(uri1)).get(0).get("progress"));
                                Log.d("aaa",uri);
                            }else if (typeReceived.equals("audio\n")){
                                ArrayList<HashMap<String, String>> progressAttay1 = dbHelper.getAudioData(String.valueOf(uri1));
                                if (progressAttay1.isEmpty()) {
                                    dbHelper.insertAudioData(fileNamereceived, uri, progressReceived);
                                    //Constant.allSendToDB.add(String.valueOf(urlOfVideo));
                                } else {
                                    dbHelper.updateAudioProgress(progressReceived, uri);
                                }

                            }else if (typeReceived.equals("pdf\n")){
                                ArrayList<HashMap<String, String>> progressAttay1 = dbHelper.getPdfData(String.valueOf(uri1));
                                if (progressAttay1.isEmpty()) {
                                    dbHelper.insertPdfData(fileNamereceived, uri, progressReceived);
                                    //Constant.allSendToDB.add(String.valueOf(urlOfVideo));
                                } else {
                                    dbHelper.updatePdfPageNumber(progressReceived, uri);
                                }

                            }


                            Log.d("aa",progressReceived);
                            Log.d("aa",typeReceived);
                            Log.d("aa",fileNamereceived);
                            break;
                        } else {
                            selectionKey.interestOps(SelectionKey.OP_READ);
                        }
                    }
                }
            }

            // Close the socket
            mSocketChannel.close();

            // If interrupted, throw an error
            if (mStop) {
                throw new IOException("transfer was cancelled");
            }

            // Indicate success
            synchronized (mTransferStatus) {
                mTransferStatus.setState(TransferStatus.State.Succeeded);
                notifyStatusChangedListeners();
            }

        } catch (IOException e) {
            synchronized (mTransferStatus) {
                mTransferStatus.setState(TransferStatus.State.Failed);
                mTransferStatus.setError(e.getMessage());
                notifyStatusChangedListeners();
            }
        }
    }
    public String readNameFile() {
        File storage = Environment.getExternalStorageDirectory();
        File downloads = new File(storage, "Download");
        File nitroShare=new File(downloads, "NitroShare");
        String  sample=new File(nitroShare, "filename").getAbsolutePath();
        File fileEvents = new File(sample);
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileEvents));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        } catch (IOException e) {
            Log.d("aa", String.valueOf(e));
        }
        String result = text.toString();
        return result;
    }
    public String readFile() {
        File storage = Environment.getExternalStorageDirectory();
        File downloads = new File(storage, "Download");
        File nitroShare=new File(downloads, "NitroShare");
        String  sample=new File(nitroShare, "sample").getAbsolutePath();
        File fileEvents = new File(sample);
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileEvents));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        } catch (IOException e) {
            Log.d("aa", String.valueOf(e));
        }
        String result = text.toString();
        return result;
    }
}
