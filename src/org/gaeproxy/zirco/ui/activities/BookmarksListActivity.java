/*
 * Zirco Browser for Android
 * 
 * Copyright (C) 2010 J. Devauchelle and contributors.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package org.gaeproxy.zirco.ui.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Browser;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.*;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import org.gaeproxy.R;
import org.gaeproxy.zirco.model.adapters.BookmarksCursorAdapter;
import org.gaeproxy.zirco.model.items.BookmarkItem;
import org.gaeproxy.zirco.providers.BookmarksProviderWrapper;
import org.gaeproxy.zirco.utils.ApplicationUtils;
import org.gaeproxy.zirco.utils.Constants;

/**
 * Bookmarks list activity.
 */
public class BookmarksListActivity extends Activity {

  private static final int MENU_ADD_BOOKMARK = Menu.FIRST;
  private static final int MENU_SORT_MODE = Menu.FIRST + 1;

  private static final int MENU_OPEN_IN_TAB = Menu.FIRST + 10;
  private static final int MENU_COPY_URL = Menu.FIRST + 11;
  private static final int MENU_SHARE = Menu.FIRST + 12;
  private static final int MENU_EDIT_BOOKMARK = Menu.FIRST + 13;
  private static final int MENU_DELETE_BOOKMARK = Menu.FIRST + 14;

  private static final int ACTIVITY_ADD_BOOKMARK = 0;
  private static final int ACTIVITY_EDIT_BOOKMARK = 1;

  private Cursor mCursor;
  private BookmarksCursorAdapter mCursorAdapter;

  private ListView mList;

  /**
   * Show a dialog for choosing the sort mode. Perform the change if required.
   */
  private void changeSortMode() {

    int currentSort = PreferenceManager.getDefaultSharedPreferences(this).getInt(
        Constants.PREFERENCES_BOOKMARKS_SORT_MODE, 0);

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setInverseBackgroundForced(true);
    builder.setIcon(android.R.drawable.ic_dialog_info);
    builder.setTitle(getResources().getString(R.string.BookmarksListActivity_MenuSortMode));
    builder.setSingleChoiceItems(
        new String[]{
            getResources().getString(R.string.BookmarksListActivity_MostUsedSortMode),
            getResources().getString(R.string.BookmarksListActivity_AlphaSortMode),
            getResources().getString(R.string.BookmarksListActivity_RecentSortMode)},
        currentSort, new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        doChangeSortMode(which);
        dialog.dismiss();
      }
    });
    builder.setCancelable(true);
    builder.setNegativeButton(R.string.Commons_Cancel, null);

    AlertDialog alert = builder.create();
    alert.show();
  }

  /**
   * Change list sort mode. Update list.
   *
   * @param sortMode The new sort mode.
   */
  private void doChangeSortMode(int sortMode) {
    Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
    editor.putInt(Constants.PREFERENCES_BOOKMARKS_SORT_MODE, sortMode);
    editor.commit();

    fillData();
  }

  /**
   * Fill the bookmark to the list UI.
   */
  private void fillData() {
    mCursor = BookmarksProviderWrapper.getStockBookmarks(
        getContentResolver(),
        PreferenceManager.getDefaultSharedPreferences(this).getInt(
            Constants.PREFERENCES_BOOKMARKS_SORT_MODE, 0));
    startManagingCursor(mCursor);

    String[] from = new String[]{Browser.BookmarkColumns.TITLE, Browser.BookmarkColumns.URL};
    int[] to = new int[]{R.id.BookmarkRow_Title, R.id.BookmarkRow_Url};

    mCursorAdapter = new BookmarksCursorAdapter(this, R.layout.bookmark_row, mCursor, from, to,
        ApplicationUtils.getFaviconSizeForBookmarks(this));

    mList.setAdapter(mCursorAdapter);

    setAnimation();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);

    switch (requestCode) {
      case ACTIVITY_EDIT_BOOKMARK:
        if (resultCode == RESULT_OK) {
          fillData();
        }
        break;
      case ACTIVITY_ADD_BOOKMARK:
        if (resultCode == RESULT_OK) {
          fillData();
        }
        break;

      default:
        break;
    }
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

    Intent i;
    BookmarkItem bookmarkItem = BookmarksProviderWrapper.getStockBookmarkById(
        getContentResolver(), info.id);

    switch (item.getItemId()) {
      case MENU_OPEN_IN_TAB:
        i = new Intent();
        i.putExtra(Constants.EXTRA_ID_NEW_TAB, true);

        if (bookmarkItem != null) {
          i.putExtra(Constants.EXTRA_ID_URL, bookmarkItem.getUrl());
        } else {
          i.putExtra(
              Constants.EXTRA_ID_URL,
              PreferenceManager.getDefaultSharedPreferences(BookmarksListActivity.this)
                  .getString(Constants.PREFERENCES_GENERAL_HOME_PAGE,
                      Constants.URL_ABOUT_START));
        }

        if (getParent() != null) {
          getParent().setResult(RESULT_OK, i);
        } else {
          setResult(RESULT_OK, i);
        }

        finish();
        return true;

      case MENU_EDIT_BOOKMARK:
        if (bookmarkItem != null) {
          i = new Intent(this, EditBookmarkActivity.class);
          i.putExtra(Constants.EXTRA_ID_BOOKMARK_ID, info.id);
          i.putExtra(Constants.EXTRA_ID_BOOKMARK_TITLE, bookmarkItem.getTitle());
          i.putExtra(Constants.EXTRA_ID_BOOKMARK_URL, bookmarkItem.getUrl());

          startActivityForResult(i, ACTIVITY_EDIT_BOOKMARK);
        }
        return true;

      case MENU_COPY_URL:
        if (bookmarkItem != null) {
          ApplicationUtils.copyTextToClipboard(this, bookmarkItem.getUrl(),
              getString(R.string.Commons_UrlCopyToastMessage));
        }
        return true;

      case MENU_SHARE:
        if (bookmarkItem != null) {
          ApplicationUtils.sharePage(this, bookmarkItem.getTitle(), bookmarkItem.getUrl());
        }
        return true;

      case MENU_DELETE_BOOKMARK:
        // mDbAdapter.deleteBookmark(info.id);
        BookmarksProviderWrapper.deleteStockBookmark(getContentResolver(), info.id);
        fillData();
        return true;
      default:
        return super.onContextItemSelected(item);
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.bookmarks_list_activity);

    setTitle(R.string.BookmarksListActivity_Title);

    View emptyView = findViewById(R.id.BookmarksListActivity_EmptyTextView);
    mList = (ListView) findViewById(R.id.BookmarksListActivity_List);

    mList.setEmptyView(emptyView);

    mList.setOnItemClickListener(new OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> l, View v, int position, long id) {
        Intent result = new Intent();
        result.putExtra(Constants.EXTRA_ID_NEW_TAB, false);

        BookmarkItem item = BookmarksProviderWrapper.getStockBookmarkById(
            getContentResolver(), id);
        if (item != null) {
          result.putExtra(Constants.EXTRA_ID_URL, item.getUrl());
        } else {
          result.putExtra(
              Constants.EXTRA_ID_URL,
              PreferenceManager.getDefaultSharedPreferences(
                  BookmarksListActivity.this).getString(
                  Constants.PREFERENCES_GENERAL_HOME_PAGE,
                  Constants.URL_ABOUT_START));
        }

        if (getParent() != null) {
          getParent().setResult(RESULT_OK, result);
        } else {
          setResult(RESULT_OK, result);
        }

        finish();
      }
    });

    registerForContextMenu(mList);

    fillData();
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);

    long id = ((AdapterContextMenuInfo) menuInfo).id;
    if (id != -1) {
      BookmarkItem item = BookmarksProviderWrapper.getStockBookmarkById(getContentResolver(),
          id);
      if (item != null) {
        menu.setHeaderTitle(item.getTitle());
      }
    }

    menu.add(0, MENU_OPEN_IN_TAB, 0, R.string.BookmarksListActivity_MenuOpenInTab);
    menu.add(0, MENU_COPY_URL, 0, R.string.BookmarksHistoryActivity_MenuCopyLinkUrl);
    menu.add(0, MENU_SHARE, 0, R.string.Main_MenuShareLinkUrl);
    menu.add(0, MENU_EDIT_BOOKMARK, 0, R.string.BookmarksListActivity_MenuEditBookmark);
    menu.add(0, MENU_DELETE_BOOKMARK, 0, R.string.BookmarksListActivity_MenuDeleteBookmark);

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);

    MenuItem item;
    item = menu.add(0, MENU_ADD_BOOKMARK, 0, R.string.BookmarksListActivity_MenuAddBookmark);
    item.setIcon(R.drawable.ic_menu_add_bookmark);

    item = menu.add(0, MENU_SORT_MODE, 0, R.string.BookmarksListActivity_MenuSortMode);
    item.setIcon(R.drawable.ic_menu_sort);

    return true;
  }

  @Override
  protected void onDestroy() {
    if (mCursor != null)
      mCursor.close();
    super.onDestroy();
  }

  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {

    switch (item.getItemId()) {
      case MENU_SORT_MODE:
        changeSortMode();
        return true;

      case MENU_ADD_BOOKMARK:
        openAddBookmarkDialog();
        return true;

      default:
        return super.onMenuItemSelected(featureId, item);
    }
  }

  /**
   * Display the add bookmark dialog.
   */
  private void openAddBookmarkDialog() {
    Intent i = new Intent(this, EditBookmarkActivity.class);

    i.putExtra(Constants.EXTRA_ID_BOOKMARK_ID, (long) -1);
    i.putExtra(Constants.EXTRA_ID_BOOKMARK_TITLE, "");
    i.putExtra(Constants.EXTRA_ID_BOOKMARK_URL, "");

    startActivityForResult(i, ACTIVITY_ADD_BOOKMARK);
  }

  /**
   * Set the list loading animation.
   */
  private void setAnimation() {
    AnimationSet set = new AnimationSet(true);

    Animation animation = new AlphaAnimation(0.0f, 1.0f);
    animation.setDuration(100);
    set.addAnimation(animation);

    animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, -1.0f,
        Animation.RELATIVE_TO_SELF, 0.0f);
    animation.setDuration(100);
    set.addAnimation(animation);

    LayoutAnimationController controller = new LayoutAnimationController(set, 0.5f);

    mList.setLayoutAnimation(controller);
  }

}
