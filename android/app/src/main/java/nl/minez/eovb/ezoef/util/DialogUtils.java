/*
 * Copyright 2023 De Staat der Nederlanden, Dienst ICT Uitvoering
 * SPDX-License-Identifier: EUPL-1.2
 */

package nl.minez.eovb.ezoef.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Switch;
import android.widget.TextView;

import com.google.common.primitives.Booleans;
import com.google.firebase.messaging.RemoteMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import nl.minez.eovb.R;
import nl.minez.eovb.ezoef.api.Auth;

public class DialogUtils {

    private static final String GROUP = "Group";
    private static final String CHILD = "Child";

    private static final HashMap<Context, AlertDialog> errorAlertDialogInstances = new HashMap<>();

    public static AlertDialog createExpandableCheckListDialog(final Context context, int titleId, final List<String> sections, final List<List<String>> items, final ArrayList<Integer> selectedIndexes, final DialogInterface.OnClickListener onPositiveClickListener) {
        final ArrayList<ArrayList<Boolean>> checkedItems = createCheckedItems(items, selectedIndexes);

        // Detect clicks and toggle selections
        final ExpandableListView expandableListView = new ExpandableListView(context);
        expandableListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                // Doing nothing
                return true;
            }
        });
        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                final CheckedTextView checkedTextView = (CheckedTextView) v.findViewById(android.R.id.text1);
                if (checkedTextView != null) {
                    checkedItems.get(groupPosition).set(childPosition, !checkedItems.get(groupPosition).get(childPosition));
                    checkedTextView.toggle();
                }
                return false;
            }
        });
        expandableListView.setGroupIndicator(null);

        updateExpandableListView(context, expandableListView, sections, items, checkedItems);

        final LayoutInflater inflater = LayoutInflater.from(context);
        final View customTitleView = inflater.inflate(R.layout.dialog_multiple_choice_custom_title, null);

        final Button allButton = (Button) customTitleView.findViewById(R.id.allButton);
        allButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int groupPosition = 0; groupPosition < checkedItems.size(); groupPosition++) {
                    final ArrayList<Boolean> childItems = checkedItems.get(groupPosition);
                    for (int childPosition = 0; childPosition < childItems.size(); childPosition++) {
                        checkedItems.get(groupPosition).set(childPosition, true);
                    }
                }

                updateExpandableListView(context, expandableListView, sections, items, checkedItems);
            }
        });

        final Button noneButton = (Button) customTitleView.findViewById(R.id.noneButton);
        noneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int groupPosition = 0; groupPosition < checkedItems.size(); groupPosition++) {
                    final ArrayList<Boolean> childItems = checkedItems.get(groupPosition);
                    for (int childPosition = 0; childPosition < childItems.size(); childPosition++) {
                        checkedItems.get(groupPosition).set(childPosition, false);
                    }
                }

                updateExpandableListView(context, expandableListView, sections, items, checkedItems);
            }
        });

        final TextView textView = (TextView) customTitleView.findViewById(R.id.textView1);
        textView.setText(titleId);

        return new AlertDialog.Builder(context)
                .setTitle(titleId)
                .setCustomTitle(customTitleView)
                .setView(expandableListView)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Update selected indexes
                        applyCheckedItemsToSelectedIndexes(selectedIndexes, checkedItems);

                        onPositiveClickListener.onClick(dialog, which);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setCancelable(false)
                .create();
    }

    private static void updateExpandableListView(Context context, ExpandableListView expandableListView, List<String> sections, List<List<String>> items, final ArrayList<ArrayList<Boolean>> checkedItems) {
        final ArrayList<Map<String, String>> groupData = new ArrayList<>();
        final ArrayList<List<Map<String, String>>> childData = new ArrayList<>();
        for (int sectionIdx = 0; sectionIdx < sections.size(); sectionIdx++) {
            final Map<String, String> curGroupMap = new HashMap<>();
            groupData.add(curGroupMap);
            curGroupMap.put(GROUP, sections.get(sectionIdx));

            final List<Map<String, String>> children = new ArrayList<>();
            for (int itemIdx = 0; itemIdx < items.get(sectionIdx).size(); itemIdx++) {
                final Map<String, String> curChildMap = new HashMap<>();
                children.add(curChildMap);
                curChildMap.put(CHILD, items.get(sectionIdx).get(itemIdx));
            }
            childData.add(children);
        }

        final SimpleExpandableListAdapter adapter = new SimpleExpandableListAdapter(context, groupData,
                R.layout.dialog_expandable_list_item,
                new String[]{GROUP}, new int[]{android.R.id.text1},
                childData, android.R.layout.simple_list_item_multiple_choice,
                new String[]{CHILD}, new int[]{android.R.id.text1}) {
            @Override
            public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
                final View childView = super.getChildView(groupPosition, childPosition, isLastChild, convertView, parent);
                if (childView != null) {
                    final CheckedTextView checkedTextView = (CheckedTextView) childView.findViewById(android.R.id.text1);
                    if (checkedTextView != null) {
                        // Fill the initial checkbox state
                        checkedTextView.setChecked(checkedItems.get(groupPosition).get(childPosition));
                    }
                }
                return childView;
            }
        };

        expandableListView.setAdapter(adapter);

        // Expand all sections by default
        for (int i = 0; i < groupData.size(); i++) {
            expandableListView.expandGroup(i);
        }
    }

    private static void applyCheckedItemsToSelectedIndexes(ArrayList<Integer> selectedIndexes, ArrayList<ArrayList<Boolean>> checkedItems) {
        selectedIndexes.clear();

        int loopIndex = 0;
        for (int sectionIdx = 0; sectionIdx < checkedItems.size(); sectionIdx++) {
            final List<Boolean> checkedItemList = checkedItems.get(sectionIdx);
            for (int itemIdx = 0; itemIdx < checkedItemList.size(); itemIdx++) {
                if (checkedItemList.get(itemIdx)) {
                    selectedIndexes.add(loopIndex);
                }
                loopIndex++;
            }
        }
    }

    private static ArrayList<ArrayList<Boolean>> createCheckedItems(List<List<String>> items, List<Integer> selectedIndexes) {
        final ArrayList<ArrayList<Boolean>> checkedSectionItems = new ArrayList<>();
        int loopIndex = 0;
        for (int sectionIdx = 0; sectionIdx < items.size(); sectionIdx++) {
            final ArrayList<Boolean> checkedItems = new ArrayList<>();

            final List<String> section = items.get(sectionIdx);
            for (int itemIdx = 0; itemIdx < section.size(); itemIdx++) {
                checkedItems.add(selectedIndexes.contains(loopIndex));
                loopIndex++;
            }

            checkedSectionItems.add(checkedItems);
        }
        return checkedSectionItems;
    }

    public static AlertDialog createCheckListDialog(Context context, int titleId, List<String> items, final ArrayList<Boolean> checkedItems, DialogInterface.OnClickListener onPositiveClickListener) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View customTitleView = inflater.inflate(R.layout.dialog_multiple_choice_custom_title, null);

        final boolean[] checked = Booleans.toArray(checkedItems);
        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setTitle(titleId)
                .setCustomTitle(customTitleView)
                .setPositiveButton(android.R.string.ok, onPositiveClickListener)
                .setNegativeButton(android.R.string.cancel, null)
                .setMultiChoiceItems(items.toArray(new String[]{}), checked, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        checkedItems.set(which, isChecked);
                    }
                })
                .setCancelable(false)
                .create();

        final Button allButton = (Button) customTitleView.findViewById(R.id.allButton);
        allButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < checkedItems.size(); i++) {
                    checked[i] = true;
                    checkedItems.set(i, true);
                    alertDialog.getListView().setItemChecked(i, true);
                }
            }
        });

        final Button noneButton = (Button) customTitleView.findViewById(R.id.noneButton);
        noneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < checkedItems.size(); i++) {
                    checked[i] = false;
                    checkedItems.set(i, false);
                    alertDialog.getListView().setItemChecked(i, false);
                }
            }
        });

        final TextView textView = (TextView) customTitleView.findViewById(R.id.textView1);
        textView.setText(titleId);

        return alertDialog;
    }

    public static AlertDialog createOnOffSwitchDialog(Context context, int titleId, int descriptionId, final AtomicBoolean on, DialogInterface.OnClickListener onPositiveClickListener) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View dialogSwitchView = inflater.inflate(R.layout.dialog_switch, null);

        final TextView textView = (TextView) dialogSwitchView.findViewById(R.id.titleTextView);
        textView.setText(context.getString(descriptionId));

        final Switch onOfSwitch = (Switch) dialogSwitchView.findViewById(R.id.onOffSwitch);
        onOfSwitch.setChecked(on.get());
        onOfSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                on.set(isChecked);
            }
        });

        return new AlertDialog.Builder(context)
                .setTitle(titleId)
                .setView(dialogSwitchView)
                .setPositiveButton(android.R.string.ok, onPositiveClickListener)
                .setNegativeButton(android.R.string.cancel, null)
                .setCancelable(false)
                .create();
    }

    public static AlertDialog createNotificationUpdatesSwitchesDialog(Context context, int titleId, int notificationsDescriptionId, final AtomicBoolean notificationsOn, int updatesDescriptionId, final AtomicBoolean updatesOn, DialogInterface.OnClickListener onPositiveClickListener) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View dialogSwitchView = inflater.inflate(R.layout.dialog_notification_updates_switches, null);

        final TextView updatesTitleTextView = (TextView) dialogSwitchView.findViewById(R.id.updatesTitleTextView);
        updatesTitleTextView.setText(context.getString(updatesDescriptionId));

        final Switch updatesOnOffSwitch = (Switch) dialogSwitchView.findViewById(R.id.updatesOnOffSwitch);
        updatesOnOffSwitch.setChecked(updatesOn.get());
        updatesOnOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updatesOn.set(isChecked);
            }
        });
        updatesOnOffSwitch.setEnabled(notificationsOn.get());

        final TextView notificationsTitleTextView = (TextView) dialogSwitchView.findViewById(R.id.notificationsTitleTextView);
        notificationsTitleTextView.setText(context.getString(notificationsDescriptionId));

        final Switch notificationsOnOffSwitch = (Switch) dialogSwitchView.findViewById(R.id.notificationsOnOffSwitch);
        notificationsOnOffSwitch.setChecked(notificationsOn.get());
        notificationsOnOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                notificationsOn.set(isChecked);
                updatesOnOffSwitch.setEnabled(isChecked);
            }
        });

        return new AlertDialog.Builder(context)
                .setTitle(titleId)
                .setView(dialogSwitchView)
                .setPositiveButton(android.R.string.ok, onPositiveClickListener)
                .setNegativeButton(android.R.string.cancel, null)
                .setCancelable(false)
                .create();
    }

    public static AlertDialog createInfoDialog(Context context, int titleId, String info, DialogInterface.OnClickListener onPositiveClickListener) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View dialogInfoView = inflater.inflate(R.layout.dialog_info, null);

        final TextView textView = (TextView) dialogInfoView.findViewById(R.id.notificationsTitleTextView);
        textView.setText(Html.fromHtml(info));
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        return new AlertDialog.Builder(context)
                .setTitle(titleId)
                .setView(dialogInfoView)
                .setPositiveButton(android.R.string.ok, onPositiveClickListener)
                .setCancelable(false)
                .create();
    }

    public static AlertDialog createNotificationAlertDialog(Context context, RemoteMessage remoteMessage, DialogInterface.OnClickListener onPositiveClickListener) {
        final String title;
        final String message;
        if (remoteMessage.getNotification() == null) {
            title = null;
            message = null;
        } else {
            title = remoteMessage.getNotification().getTitle();
            message = remoteMessage.getNotification().getBody();
        }

        return new AlertDialog.Builder(context)
                .setTitle(title == null ? context.getString(R.string.notification) : title)
                .setMessage(message == null ? "" : message)
                .setPositiveButton(android.R.string.ok, onPositiveClickListener)
                .setCancelable(false)
                .create();
    }

    public static AlertDialog createErrorAlertDialog(Context context, String title, String message) {
        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(android.R.string.ok, null)
                .setCancelable(false)
                .create();
        errorAlertDialogInstances.put(context, alertDialog);
        return alertDialog;
    }

    public static AlertDialog createErrorAlertDialog(Context context, int title, int message) {
        final AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(android.R.string.ok, null)
                .setCancelable(false)
                .create();
        errorAlertDialogInstances.put(context, alertDialog);
        return alertDialog;
    }

    public static AlertDialog createErrorAlertDialog(Context context, String message) {
        return createErrorAlertDialog(context, context.getString(R.string.oops), message);
    }

    public static AlertDialog createErrorAlertDialog(Context context, int message) {
        return createErrorAlertDialog(context, R.string.oops, message);
    }

    public static AlertDialog createAuthErrorAlertDialog(Context context, Auth.ErrorCode errorCode, String errorMessage, DialogInterface.OnClickListener onRegisterClickListener) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.access)
                .setMessage(errorMessage)
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(false);

//        if (!BuildConfig.TARGET_PUBLIC && (errorCode == Auth.ErrorCode.FORBIDDEN || errorCode == Auth.ErrorCode.BLOCKED || errorCode == Auth.ErrorCode.NOT_REGISTERED)
//                && onRegisterClickListener != null) {
//            builder.setNegativeButton(R.string.register, onRegisterClickListener);
//        }

        final AlertDialog alertDialog = builder.create();
        errorAlertDialogInstances.put(context, alertDialog);

        return alertDialog;
    }

    public static boolean errorAlertDialogIsShowing(Context context) {
        final AlertDialog alertDialog = errorAlertDialogInstances.get(context);
        if (alertDialog == null) {
            return false;
        }
        return alertDialog.isShowing();
    }

    public static void dismissErrorAlertDialogIfShown(Context context) {
        final AlertDialog alertDialog = errorAlertDialogInstances.get(context);
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();

            errorAlertDialogInstances.remove(context);
        }
    }

}