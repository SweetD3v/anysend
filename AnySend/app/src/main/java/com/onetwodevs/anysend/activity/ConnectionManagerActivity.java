package com.onetwodevs.anysend.activity;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.FragmentTransaction;

import com.onetwodevs.anysend.R;
import com.onetwodevs.anysend.app.Activity;
import com.onetwodevs.anysend.database.AccessDatabase;
import com.onetwodevs.anysend.dialog.ManualIpAddressConnectionDialog;
import com.onetwodevs.anysend.fragment.BarcodeConnectFragment;
import com.onetwodevs.anysend.fragment.HotspotManagerFragment;
import com.onetwodevs.anysend.fragment.NetworkDeviceListFragment;
import com.onetwodevs.anysend.fragment.NetworkManagerFragment;
import com.onetwodevs.anysend.fragment.TransferAssigneeListFragment;
import com.onetwodevs.anysend.object.NetworkDevice;
import com.onetwodevs.anysend.object.TransferGroup;
import com.onetwodevs.anysend.service.CommunicationService;
import com.onetwodevs.anysend.service.WorkerService;
import com.onetwodevs.anysend.task.AddDeviceRunningTask;
import com.onetwodevs.anysend.ui.UIConnectionUtils;
import com.onetwodevs.anysend.ui.UITask;
import com.onetwodevs.anysend.ui.callback.NetworkDeviceSelectedListener;
import com.onetwodevs.anysend.ui.callback.TitleSupport;
import com.onetwodevs.anysend.util.AppUtils;
import com.onetwodevs.anysend.util.ConnectionUtils;
import com.onetwodevs.anysend.util.NetworkDeviceLoader;
import com.genonbeta.android.framework.ui.callback.SnackbarSupport;
import com.genonbeta.android.framework.util.Interrupter;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.snackbar.Snackbar;

import static com.onetwodevs.anysend.service.CommunicationService.EXTRA_GROUP_ID;

public class ConnectionManagerActivity
        extends Activity
        implements SnackbarSupport, WorkerService.OnAttachListener {
    public static final String ACTION_CHANGE_FRAGMENT = "com.onetwodevs.intent.action.CONNECTION_MANAGER_CHANGE_FRAGMENT";
    public static final String EXTRA_FRAGMENT_ENUM = "extraFragmentEnum";
    public static final String EXTRA_DEVICE_ID = "extraDeviceId";
    public static final String EXTRA_CONNECTION_ADAPTER = "extraConnectionAdapter";
    public static final String EXTRA_REQUEST_TYPE = "extraRequestType";
    public static final String EXTRA_ACTIVITY_SUBTITLE = "extraActivitySubtitle";

    private final IntentFilter mFilter = new IntentFilter();
    private HotspotManagerFragment mHotspotManagerFragment;
    private BarcodeConnectFragment mBarcodeConnectFragment;
    private NetworkManagerFragment mNetworkManagerFragment;
    private NetworkDeviceListFragment mDeviceListFragment;
    private OptionsFragment mOptionsFragment;
    private AppBarLayout mAppBarLayout;
    private CollapsingToolbarLayout mToolbarLayout;
    private ProgressBar mProgressBar;
    private String mTitleProvided;
    private RequestType mRequestType = RequestType.RETURN_RESULT;
    private TransferGroup mGroup = null;
    private AddDeviceRunningTask mTask;

    private final NetworkDeviceSelectedListener mDeviceSelectionListener = new NetworkDeviceSelectedListener() {
        @Override
        public boolean onNetworkDeviceSelected(NetworkDevice networkDevice, NetworkDevice.Connection connection) {
            if (mRequestType.equals(RequestType.RETURN_RESULT)) {
                setResult(RESULT_OK, new Intent()
                        .putExtra(EXTRA_DEVICE_ID, networkDevice.deviceId)
                        .putExtra(EXTRA_CONNECTION_ADAPTER, connection.adapterName));

                finish();
            } else {
                ConnectionUtils connectionUtils = ConnectionUtils.getInstance(ConnectionManagerActivity.this);
                UIConnectionUtils uiConnectionUtils = new UIConnectionUtils(connectionUtils, ConnectionManagerActivity.this);

                UITask uiTask = new UITask() {
                    @Override
                    public void updateTaskStarted(Interrupter interrupter) {
                        mProgressBar.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void updateTaskStopped() {
                        mProgressBar.setVisibility(View.GONE);
                    }
                };

                NetworkDeviceLoader.OnDeviceRegisteredListener registeredListener = new NetworkDeviceLoader.OnDeviceRegisteredListener() {
                    @Override
                    public void onDeviceRegistered(AccessDatabase database, final NetworkDevice device, final NetworkDevice.Connection connection) {
                        createSnackbar(R.string.mesg_completing).show();
                    }
                };

                uiConnectionUtils.makeAcquaintance(ConnectionManagerActivity.this, uiTask,
                        connection.ipAddress, -1, registeredListener);
            }

            return true;
        }

        @Override
        public boolean isListenerEffective() {
            return true;
        }
    };

    @Override
    protected void onPreviousRunningTask(@Nullable WorkerService.RunningTask task) {
        super.onPreviousRunningTask(task);

        if (task instanceof AddDeviceRunningTask) {
            mTask = ((AddDeviceRunningTask) task);
            mTask.setAnchorListener(new AddDevicesToTransferActivity());
        }
    }

    @Override
    public void onAttachedToTask(WorkerService.RunningTask task) {
        takeOnProcessMode();
    }

    public void takeOnProcessMode() {
        if (mTask != null)
            mTask.getInterrupter().interrupt();
    }

    /*public void doCommunicate(final NetworkDevice device, final NetworkDevice.Connection connection) {
        AddDeviceRunningTask task = new AddDeviceRunningTask(mGroup, device, connection);

        task.setTitle(getString(R.string.mesg_communicating))
                .setAnchorListener(new AddDevicesToTransferActivity())
                .setContentIntent(this, getIntent())
                .run(this);

        attachRunningTask(task);
    }*/

    /*@Override
    public void onAttachedToTask(WorkerService.RunningTask task) {
        takeOnProcessMode();
    }

    public boolean checkGroupIntegrity() {
        try {
            if (getIntent() == null || !getIntent().hasExtra(EXTRA_GROUP_ID))
                throw new Exception(getString(R.string.text_empty));

            mGroup = new TransferGroup(getIntent().getLongExtra(EXTRA_GROUP_ID, -1));

            try {
                getDatabase().reconstruct(mGroup);
            } catch (Exception e) {
                throw new Exception(getString(R.string.mesg_notValidTransfer));
            }

            return true;
        } catch (Exception e) {
            Toast.makeText(AddDevicesToTransferActivity.this,
                    e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }

        return false;
    }

    public void takeOnProcessMode() {
        if (mTask != null)
            mTask.getInterrupter().interrupt();
    }

    @Override
    protected void onPreviousRunningTask(@Nullable WorkerService.RunningTask task) {
        super.onPreviousRunningTask(task);

        if (task instanceof AddDeviceRunningTask) {
            mTask = ((AddDeviceRunningTask) task);
            mTask.setAnchorListener(this);
        }
    }*/

    @Override
    protected void onStart() {
        super.onStart();
        checkForTasks();
    }

    public boolean checkGroupIntegrity() {
        try {
            if (getIntent() == null || !getIntent().hasExtra(AddDevicesToTransferActivity.EXTRA_GROUP_ID))
                throw new Exception(getString(R.string.text_empty));

            mGroup = new TransferGroup(getIntent().getLongExtra(AddDevicesToTransferActivity.EXTRA_GROUP_ID, -1));

            try {
                getDatabase().reconstruct(mGroup);
            } catch (Exception e) {
                throw new Exception(getString(R.string.mesg_notValidTransfer));
            }

            return true;
        } catch (Exception e) {
            Toast.makeText(ConnectionManagerActivity.this,
                    e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }

        return false;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_CHANGE_FRAGMENT.equals(intent.getAction())
                    && intent.hasExtra(EXTRA_FRAGMENT_ENUM)) {
                String fragmentEnum = intent.getStringExtra(EXTRA_FRAGMENT_ENUM);

                try {
                    AvailableFragment value = AvailableFragment.valueOf(fragmentEnum);

                    if (AvailableFragment.EnterIpAddress.equals(value))
                        showEnterIpAddressDialog();
                    else
                        setFragment(value);
                } catch (Exception e) {
                    // do nothing
                }
            } else if (mRequestType.equals(RequestType.RETURN_RESULT)) {
                if (CommunicationService.ACTION_DEVICE_ACQUAINTANCE.equals(intent.getAction())
                        && intent.hasExtra(CommunicationService.EXTRA_DEVICE_ID)
                        && intent.hasExtra(CommunicationService.EXTRA_CONNECTION_ADAPTER_NAME)) {
                    NetworkDevice device = new NetworkDevice(intent.getStringExtra(CommunicationService.EXTRA_DEVICE_ID));
                    NetworkDevice.Connection connection = new NetworkDevice.Connection(device.deviceId, intent.getStringExtra(CommunicationService.EXTRA_CONNECTION_ADAPTER_NAME));
                    try {
                        AppUtils.isConnected = true;

                        AppUtils.getDatabase(ConnectionManagerActivity.this).reconstruct(device);
                        AppUtils.getDatabase(ConnectionManagerActivity.this).reconstruct(connection);

                        mDeviceSelectionListener.onNetworkDeviceSelected(device, connection);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else if (mRequestType.equals(RequestType.MAKE_ACQUAINTANCE)) {
                if (CommunicationService.ACTION_INCOMING_TRANSFER_READY.equals(intent.getAction())
                        && intent.hasExtra(EXTRA_GROUP_ID)) {
                    ViewTransferActivity.startInstance(ConnectionManagerActivity.this,
                            intent.getLongExtra(EXTRA_GROUP_ID, -1));
                    finish();
                }
            }
        }
    };

    public static void startInstance(Context context, long groupId) {

        context.startActivity(new Intent(context, ConnectionManagerActivity.class)
                .putExtra(ConnectionManagerActivity.EXTRA_ACTIVITY_SUBTITLE, context.getString(R.string.text_addDevicesToTransfer))
                .putExtra(EXTRA_GROUP_ID, groupId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_connection_manager);

        if (AppUtils.isHotspotOn) {
            if (!checkGroupIntegrity())
                return;
            Bundle assigneeFragmentArgs = new Bundle();
            assigneeFragmentArgs.putLong(TransferAssigneeListFragment.ARG_GROUP_ID, mGroup.groupId);
            assigneeFragmentArgs.putBoolean(TransferAssigneeListFragment.ARG_USE_HORIZONTAL_VIEW, false);

            TransferAssigneeListFragment assigneeListFragment =
                    (TransferAssigneeListFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.assigneeListFragment);

            if (assigneeListFragment == null) {
                assigneeListFragment = (TransferAssigneeListFragment) Fragment
                        .instantiate(this, TransferAssigneeListFragment.class.getName(), assigneeFragmentArgs);

                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

                transaction.add(R.id.assigneeListFragment, assigneeListFragment);
                transaction.commit();
            }
        }

        if (AppUtils.isSender) {
            findViewById(R.id.assigneeListFragment).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.assigneeListFragment).setVisibility(View.GONE);
        }

        FragmentFactory factory = getSupportFragmentManager().getFragmentFactory();
        Toolbar toolbar = findViewById(R.id.toolbar);
        mAppBarLayout = findViewById(R.id.app_bar);
        mProgressBar = findViewById(R.id.activity_connection_establishing_progress_bar);
        mToolbarLayout = findViewById(R.id.toolbar_layout);
        mOptionsFragment = (OptionsFragment) factory.instantiate(getClassLoader(), OptionsFragment.class.getName());
        mBarcodeConnectFragment = (BarcodeConnectFragment) factory.instantiate(getClassLoader(), BarcodeConnectFragment.class.getName());
        mHotspotManagerFragment = (HotspotManagerFragment) factory.instantiate(getClassLoader(), HotspotManagerFragment.class.getName());
        mNetworkManagerFragment = (NetworkManagerFragment) factory.instantiate(getClassLoader(), NetworkManagerFragment.class.getName());
        mDeviceListFragment = (NetworkDeviceListFragment) factory.instantiate(getClassLoader(), NetworkDeviceListFragment.class.getName());

        mFilter.addAction(ACTION_CHANGE_FRAGMENT);
        mFilter.addAction(CommunicationService.ACTION_DEVICE_ACQUAINTANCE);
        mFilter.addAction(CommunicationService.ACTION_INCOMING_TRANSFER_READY);

        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (getIntent() != null) {
            if (getIntent().hasExtra(EXTRA_REQUEST_TYPE))
                try {
                    mRequestType = RequestType.valueOf(getIntent().getStringExtra(EXTRA_REQUEST_TYPE));
                } catch (Exception e) {
                    // do nothing
                }

            if (getIntent().hasExtra(EXTRA_ACTIVITY_SUBTITLE))
                mTitleProvided = getIntent().getStringExtra(EXTRA_ACTIVITY_SUBTITLE);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        checkFragment();
//        if (!checkGroupIntegrity())
//            finish();
        registerReceiver(mReceiver, mFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onBackPressed() {
        if (getShowingFragment() instanceof OptionsFragment) {

            finish();
        } else {
            setFragment(AvailableFragment.Options);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
        } else {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }

    public void applyViewChanges(Fragment fragment, String mTitleProvided) {
        boolean isOptions = fragment instanceof OptionsFragment;

        if (fragment instanceof DeviceSelectionSupport)
            ((DeviceSelectionSupport) fragment).setDeviceSelectedListener(mDeviceSelectionListener);

        if (getSupportActionBar() != null) {
            CharSequence titleCurrent = fragment instanceof TitleSupport
                    ? ((TitleSupport) fragment).getTitle(ConnectionManagerActivity.this)
                    : getString(R.string.text_connectDevices);

            if (isOptions)
                mToolbarLayout.setTitle(mTitleProvided != null ? mTitleProvided : titleCurrent);
            else
                mToolbarLayout.setTitle(titleCurrent);
        }

        mAppBarLayout.setExpanded(isOptions, true);
    }

    private void checkFragment() {
        Fragment currentFragment = getShowingFragment();

        if (currentFragment == null)
            setFragment(AvailableFragment.Options);
        else
            applyViewChanges(currentFragment, mTitleProvided);
    }

    @Override
    public Snackbar createSnackbar(int resId, Object... objects) {
        return Snackbar.make(findViewById(R.id.activity_connection_establishing_content_view), getString(resId, objects), Snackbar.LENGTH_LONG);
    }

    @IdRes
    public AvailableFragment getShowingFragmentId() {
        Fragment fragment = getShowingFragment();

        if (fragment instanceof BarcodeConnectFragment)
            return AvailableFragment.ScanQrCode;
        else if (fragment instanceof HotspotManagerFragment)
            return AvailableFragment.CreateHotspot;
        else if (fragment instanceof NetworkManagerFragment)
            return AvailableFragment.UseExistingNetwork;
        else if (fragment instanceof NetworkDeviceListFragment)
            return AvailableFragment.UseKnownDevice;

        // Probably OptionsFragment
        return AvailableFragment.Options;
    }

    @Nullable
    public Fragment getShowingFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.activity_connection_establishing_content_view);
    }

    public void setFragment(AvailableFragment fragment) {
        @Nullable
        Fragment activeFragment = getShowingFragment();
        Fragment fragmentCandidate = null;

        switch (fragment) {
            case ScanQrCode:
                //fragmentCandidate = mBarcodeConnectFragment;
                if (mOptionsFragment.isAdded())
                    mOptionsFragment.startCodeScanner();
                return;
            case CreateHotspot:
                fragmentCandidate = mHotspotManagerFragment;
                break;
            case UseExistingNetwork:
                fragmentCandidate = mNetworkManagerFragment;
                break;
            case UseKnownDevice:
                fragmentCandidate = mDeviceListFragment;
                break;
            default:
                fragmentCandidate = mOptionsFragment;
        }

        if (activeFragment == null || fragmentCandidate != activeFragment) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            if (activeFragment != null)
                transaction.remove(activeFragment);

            if (activeFragment != null && fragmentCandidate instanceof OptionsFragment) ;

            if (AppUtils.isSender) {
                transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left);
                transaction.add(R.id.activity_connection_establishing_content_view, fragmentCandidate);
                transaction.add(R.id.newFragment, mHotspotManagerFragment);
                transaction.commit();
            } else {
                transaction.setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left);
                transaction.add(R.id.activity_connection_establishing_content_view, fragmentCandidate);
                transaction.commit();
            }
        }

        applyViewChanges(fragmentCandidate, mTitleProvided);
    }

    protected void showEnterIpAddressDialog() {
        ConnectionUtils connectionUtils = ConnectionUtils.getInstance(this);
        UIConnectionUtils uiConnectionUtils = new UIConnectionUtils(connectionUtils, this);
        new ManualIpAddressConnectionDialog(this, uiConnectionUtils, mDeviceSelectionListener).show();
    }

    public enum RequestType {
        RETURN_RESULT,
        MAKE_ACQUAINTANCE
    }

    public enum AvailableFragment {
        Options,
        UseExistingNetwork,
        UseKnownDevice,
        ScanQrCode,
        CreateHotspot,
        EnterIpAddress
    }

    public interface DeviceSelectionSupport {
        void setDeviceSelectedListener(NetworkDeviceSelectedListener listener);
    }

    public static class OptionsFragment
            extends com.genonbeta.android.framework.app.Fragment
            implements DeviceSelectionSupport {
        public static final int REQUEST_CHOOSE_DEVICE = 100;

        private NetworkDeviceSelectedListener mListener;

        CardView cardView, createHotspotBtn;
        Button showHideTxt;
        FrameLayout shareOnPC;
        FrameLayout newFragment;
        private HotspotManagerFragment mHotspotManagerFragment;
        FragmentTransaction transaction;


        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.layout_connection_options_fragment, container, false);

            cardView = view.findViewById(R.id.cardView);
            showHideTxt = view.findViewById(R.id.show_hide_text);
            newFragment = view.findViewById(R.id.newFragment);
            createHotspotBtn = view.findViewById(R.id.createHotspotBtn);

            if (AppUtils.isReceiver) {
                view.findViewById(R.id.barcodeScannerFragment).setVisibility(View.VISIBLE);
                createHotspotBtn.setVisibility(View.GONE);
            } else {
                createHotspotBtn.setVisibility(View.VISIBLE);
                FragmentFactory factory = getActivity().getSupportFragmentManager().getFragmentFactory();
                mHotspotManagerFragment = (HotspotManagerFragment) factory.instantiate(ClassLoader.getSystemClassLoader(), HotspotManagerFragment.class.getName());
                view.findViewById(R.id.barcodeScannerFragment).setVisibility(View.GONE);
            }

            final ObjectAnimator anim;
            anim = ObjectAnimator.ofInt(showHideTxt, "backgroundColor",
                    getContext().getResources().getColor(R.color.zxing_transparent),
                    getContext().getResources().getColor(R.color.dark_colorSecondary),
                    getContext().getResources().getColor(R.color.zxing_transparent));
            anim.setDuration(500);
            anim.setEvaluator(new ArgbEvaluator());
            anim.setRepeatMode(ValueAnimator.REVERSE);
            anim.setRepeatCount(5);
            anim.start();

            showHideTxt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (anim.isRunning()) {
                        anim.end();
                    }

                    transaction = getActivity().getSupportFragmentManager().beginTransaction();

                    if (showHideTxt.getText().toString().equals("Create")) {

                        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                        builder.setTitle("Create hotpot")
                                .setMessage(R.string.create_hotspot)
                                .setPositiveButton(R.string.butn_yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        newFragment.setVisibility(View.VISIBLE);
                                        showHideTxt.setText("Cancel");
                                        transaction.setCustomAnimations(R.anim.enter_from_up, R.anim.exit_to_down);
                                        transaction.replace(R.id.newFragment, mHotspotManagerFragment);
                                        transaction.commit();
                                    }
                                })
                                .setNegativeButton(R.string.butn_no, null);

                        builder.show();


                    } else {
                        newFragment.setVisibility(View.GONE);
                        showHideTxt.setText("Create");
                        transaction.replace(R.id.newFragment, new Fragment());
                        transaction.commit();
                    }
                }
            });

//            showHideTxt.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    if (showHideTxt.getText().toString().equals("Show")) {
//                        cardView.setVisibility(View.VISIBLE);
//                        showHideTxt.setText("Hide");
//
//                    } else {
//                        cardView.setVisibility(View.GONE);
//                        showHideTxt.setText("Show");
//                    }
//                }
//            });

//            if (AppUtils.isConnected) {
//                cardView.setVisibility(View.GONE);
//            } else {
//            View.OnClickListener listener = new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    switch (v.getId()) {
//                        case R.id.connection_option_devices:
//                            updateFragment(AvailableFragment.UseKnownDevice);
//                            break;
//                        case R.id.connection_option_hotspot:
//                            updateFragment(AvailableFragment.CreateHotspot);
//                            break;
//                        case R.id.connection_option_network:
//                            updateFragment(AvailableFragment.UseExistingNetwork);
//                            break;
//                        case R.id.connection_option_manual_ip:
//                            updateFragment(AvailableFragment.EnterIpAddress);
//                            break;
//                        case R.id.connection_option_scan:
//                            startCodeScanner();
//                    }
//                }
//            };
//
//            view.findViewById(R.id.connection_option_devices).setOnClickListener(listener);
//            view.findViewById(R.id.connection_option_hotspot).setOnClickListener(listener);
//            view.findViewById(R.id.connection_option_network).setOnClickListener(listener);
//            view.findViewById(R.id.connection_option_scan).setOnClickListener(listener);
//            view.findViewById(R.id.connection_option_manual_ip).setOnClickListener(listener);
//
//            view.findViewById(R.id.connection_option_guide).setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    new ConnectionSetUpAssistant(getActivity())
//                            .startShowing();
//                }
//            });
//            }

            return view;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode == REQUEST_CHOOSE_DEVICE) {
                if (resultCode == RESULT_OK && data != null) {
                    try {
                        NetworkDevice device = new NetworkDevice(data.getStringExtra(BarcodeScannerActivity.EXTRA_DEVICE_ID));
                        AppUtils.getDatabase(getContext()).reconstruct(device);
                        NetworkDevice.Connection connection = new NetworkDevice.Connection(device.deviceId, data.getStringExtra(BarcodeScannerActivity.EXTRA_CONNECTION_ADAPTER));
                        AppUtils.getDatabase(getContext()).reconstruct(connection);

                        if (mListener != null)
                            mListener.onNetworkDeviceSelected(device, connection);
                    } catch (Exception e) {
                        // do nothing
                    }
                }
            }

        }

        private void startCodeScanner() {
            startActivityForResult(new Intent(getActivity(), BarcodeScannerActivity.class),
                    REQUEST_CHOOSE_DEVICE);
        }

        public void updateFragment(AvailableFragment fragment) {
            if (getContext() != null)
                getContext().sendBroadcast(new Intent(ACTION_CHANGE_FRAGMENT)
                        .putExtra(EXTRA_FRAGMENT_ENUM, fragment.toString()));
        }

        @Override
        public void setDeviceSelectedListener(NetworkDeviceSelectedListener listener) {
            mListener = listener;
        }
    }
}
