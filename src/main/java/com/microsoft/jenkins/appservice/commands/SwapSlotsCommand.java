package com.microsoft.jenkins.appservice.commands;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.jenkins.appservice.Messages;
import com.microsoft.jenkins.appservice.util.Constants;
import com.microsoft.jenkins.azurecommons.command.CommandState;
import com.microsoft.jenkins.azurecommons.command.IBaseCommandData;
import com.microsoft.jenkins.azurecommons.command.ICommand;
import org.apache.commons.lang.StringUtils;

import java.util.NoSuchElementException;

public class SwapSlotsCommand implements ICommand<SwapSlotsCommand.ISwapSlotsCommandData> {

    @Override
    public void execute(ISwapSlotsCommandData context) {
        String sourceSlotName = context.getSourceSlotName();
        String targetSlotName = context.getTargetSlotName();
        WebApp webApp = context.getWebApp();
        if (StringUtils.isBlank(sourceSlotName) || StringUtils.isBlank(targetSlotName)) {
            context.logError(Messages.Slot_name_blank(sourceSlotName, targetSlotName));
            context.setCommandState(CommandState.HasError);
            return;
        }
        if (sourceSlotName.equals(targetSlotName)) {
            context.logError(Messages.Slot_name_same());
            context.setCommandState(CommandState.HasError);
            return;
        }

        WebAppBase sourceSlot;
        try {
            sourceSlot = Constants.PRODUCTION_SLOT_NAME.equals(sourceSlotName)
                    ? webApp : webApp.deploymentSlots().getByName(sourceSlotName);
            if (!Constants.PRODUCTION_SLOT_NAME.equals(targetSlotName)) {
                webApp.deploymentSlots().getByName(targetSlotName);
            }
        } catch (NoSuchElementException e) {
            context.logError(Messages.Slot_not_exist(sourceSlotName, targetSlotName, webApp.name()));
            context.setCommandState(CommandState.HasError);
            return;
        }
        sourceSlot.swap(targetSlotName);

        context.setCommandState(CommandState.Success);
    }

    public interface ISwapSlotsCommandData extends IBaseCommandData {
        String getSourceSlotName();

        String getTargetSlotName();

        WebApp getWebApp();
    }
}
