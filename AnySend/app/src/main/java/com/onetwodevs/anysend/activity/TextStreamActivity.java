package com.onetwodevs.anysend.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.onetwodevs.anysend.R;
import com.onetwodevs.anysend.app.Activity;
import com.onetwodevs.anysend.fragment.TextStreamListFragment;
import com.onetwodevs.anysend.ui.callback.PowerfulActionModeSupport;
import com.genonbeta.android.framework.widget.PowerfulActionMode;

public class TextStreamActivity
        extends Activity
        implements PowerfulActionModeSupport
{
    private PowerfulActionMode mActionMode;
    private TextStreamListFragment mStreamListFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_stream);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mActionMode = findViewById(R.id.action_mode);
        mStreamListFragment = (TextStreamListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.activity_text_stream_fragment);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mActionMode.setOnSelectionTaskListener(new PowerfulActionMode.OnSelectionTaskListener()
        {
            @Override
            public void onSelectionTask(boolean started, PowerfulActionMode actionMode)
            {
                toolbar.setVisibility(!started ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public void onBackPressed()
    {
        if (mActionMode.hasActive(mStreamListFragment.getSelectionCallback()))
            mActionMode.finish(mStreamListFragment.getSelectionCallback());
        else
            super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == android.R.id.home)
            finish();
        else
            return super.onOptionsItemSelected(item);

        return true;
    }

    @Override
    public PowerfulActionMode getPowerfulActionMode()
    {
        return mActionMode;
    }
}
