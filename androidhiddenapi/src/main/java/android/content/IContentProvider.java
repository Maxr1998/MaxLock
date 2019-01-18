package android.content;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ICancellationSignal;
import android.os.IInterface;
import android.os.RemoteException;

public interface IContentProvider extends IInterface {
    Cursor query(String callingPkg, Uri url, String[] projection, Bundle queryArgs, ICancellationSignal cancellationSignal) throws RemoteException;

    int bulkInsert(String callingPkg, Uri url, ContentValues[] initialValues) throws RemoteException;
}