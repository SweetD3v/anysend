package com.onetwodevs.anysend.dialog;

import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.onetwodevs.anysend.R;
import com.onetwodevs.anysend.activity.ViewTransferActivity;
import com.onetwodevs.anysend.graphics.drawable.TextDrawable;
import com.onetwodevs.anysend.object.ShowingAssignee;
import com.onetwodevs.anysend.object.TransferGroup;
import com.onetwodevs.anysend.object.TransferObject;
import com.onetwodevs.anysend.util.AppUtils;
import com.onetwodevs.anysend.util.NetworkDeviceLoader;
import com.onetwodevs.anysend.util.TransferUtils;
import com.genonbeta.android.database.exception.ReconstructionFailedException;

import java.util.ArrayList;
import java.util.List;

public class ToggleMultipleTransferDialog extends AlertDialog.Builder
{
    private List<ShowingAssignee> mList = new ArrayList<>();
    private List<String> mActiveList = new ArrayList<>();
    private LayoutInflater mInflater;
    private TextDrawable.IShapeBuilder mIconBuilder;

    public ToggleMultipleTransferDialog(@NonNull final ViewTransferActivity activity,
                                        final TransferGroup group,
                                        final List<ShowingAssignee> deviceList,
                                        final List<String> activeList,
                                        TransferGroup.Index transferIndex)
    {
        super(activity);

        mInflater = LayoutInflater.from(activity);
        mIconBuilder = AppUtils.getDefaultIconBuilder(activity);
        mList.addAll(deviceList);
        mActiveList.addAll(activeList);

        setAdapter(new ActiveListAdapter(), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                ShowingAssignee assignee = mList.get(which);

                if (mActiveList.contains(assignee.deviceId))
                    TransferUtils.pauseTransfer(activity, group.groupId, assignee.deviceId);
                else
                    TransferUtils.startTransfer(activity, group, assignee,
                            TransferObject.Type.OUTGOING);
            }
        });

        setNegativeButton(R.string.butn_close, null);

        if (transferIndex.outgoingCount > 0)
            setNeutralButton(R.string.butn_addDevices, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    activity.startDeviceAddingActivity();
                }
            });

        if (transferIndex.incomingCount > 0)
            setPositiveButton(R.string.butn_receive, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    TransferObject object = TransferUtils.fetchFirstTransfer(getContext(),
                            group.groupId, TransferObject.Type.INCOMING);

                    if (object != null) {
                        TransferGroup.Assignee assignee = new TransferGroup.Assignee(group.groupId,
                                object.deviceId);

                        try {
                            AppUtils.getDatabase(getContext()).reconstruct(assignee);
                            TransferUtils.startTransferWithTest(activity, group,
                                    assignee, TransferObject.Type.INCOMING);
                        } catch (ReconstructionFailedException e) {
                            // Failed, do nothing!
                        }
                    }
                }
            });
    }

    private class ActiveListAdapter extends BaseAdapter
    {
        @Override
        public int getCount()
        {
            return mList.size();
        }

        @Override
        public Object getItem(int position)
        {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position)
        {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            if (convertView == null)
                convertView = mInflater.inflate(R.layout.list_toggle_transfer, parent,
                        false);

            ShowingAssignee assignee = (ShowingAssignee) getItem(position);
            ImageView image = convertView.findViewById(R.id.image);
            TextView text = convertView.findViewById(R.id.text);
            ImageView actionImage = convertView.findViewById(R.id.actionImage);

            text.setText(assignee.device.nickname);
            actionImage.setImageResource(mActiveList.contains(assignee.deviceId)
                    ? R.drawable.ic_pause_white_24dp
                    : R.drawable.ic_arrow_up_white_24dp);
            NetworkDeviceLoader.showPictureIntoView(assignee.device, image, mIconBuilder);

            return convertView;
        }
    }
}
