package com.gearswitch;

/*
 * Copyright (c) 2018 kulers
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;

class GearsInventoryTagsOverlay extends WidgetItemOverlay
{
    private final ItemManager itemManager;
    private final GearSwitchAlertPlugin plugin;
    private final GearSwitchAlertConfig config;
    private final Cache<Long, Image> fillCache;
    private final Cache<Integer, GearTagSettings> tagCache;

    @Inject
    private GearsInventoryTagsOverlay(ItemManager itemManager, GearSwitchAlertPlugin plugin, GearSwitchAlertConfig config)
    {
        this.itemManager = itemManager;
        this.plugin = plugin;
        this.config = config;
//        showOnEquipment();
        showOnInventory();
        showOnInterfaces(
                WidgetID.CHAMBERS_OF_XERIC_STORAGE_UNIT_INVENTORY_GROUP_ID,
                WidgetID.CHAMBERS_OF_XERIC_STORAGE_UNIT_PRIVATE_GROUP_ID,
                WidgetID.CHAMBERS_OF_XERIC_STORAGE_UNIT_SHARED_GROUP_ID,
                WidgetID.GRAVESTONE_GROUP_ID
        );
        fillCache = CacheBuilder.newBuilder()
                .concurrencyLevel(1)
                .maximumSize(32)
                .build();
        tagCache = CacheBuilder.newBuilder()
                .concurrencyLevel(1)
                .maximumSize(32)
                .build();
    }

    @Override
    public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
    {
        final GearTagSettings gearTagSettings = getTag(itemId);
        if (gearTagSettings == null)
        {
            return;
        }

        AttackType attackType = plugin.getAttackType();
        Color color = null;
        switch (attackType) {
            case RANGE:
                if(!gearTagSettings.isRangeGear)
                    return;

                color = config.defaultColourRanged();
                break;
            case MAGIC:
                if(!gearTagSettings.isMagicGear)
                    return;

                color = config.defaultColourMagic();
                break;
            case MELEE:
                if(!gearTagSettings.isMeleeGear)
                    return;

                color = config.defaultColourMelee();
                break;
            case OTHER:
                return;
        }

        Rectangle bounds = widgetItem.getCanvasBounds();
        if (config.showTagOutline())
        {
            final BufferedImage outline = itemManager.getItemOutline(itemId, widgetItem.getQuantity(), color);
            graphics.drawImage(outline, (int) bounds.getX(), (int) bounds.getY(), null);
        }

        if (config.showTagFill())
        {
            final Image image = getFillImage(color, widgetItem.getId(), widgetItem.getQuantity());
            graphics.drawImage(image, (int) bounds.getX(), (int) bounds.getY(), null);
        }

        if (config.showTagUnderline())
        {
            int heightOffSet = (int) bounds.getY() + (int) bounds.getHeight() + 2;
            graphics.setColor(color);
            graphics.drawLine((int) bounds.getX()-2, heightOffSet, (int) bounds.getX()-2 + (int) bounds.getWidth(), heightOffSet);
        }

        if (config.showBoxAround())
        {
            int heightOffSet = (int) bounds.getY() + (int) bounds.getHeight();
            graphics.setColor(color);
            graphics.drawLine((int) bounds.getX()-2, (int) bounds.getY(), (int) bounds.getX()-2 + (int) bounds.getWidth(), (int) bounds.getY());
            graphics.drawLine((int) bounds.getX()-2, (int) bounds.getY(), (int) bounds.getX()-2, heightOffSet);
            graphics.drawLine((int) bounds.getX()-2 + (int) bounds.getWidth(), (int) bounds.getY(), (int) bounds.getX()-1 + (int) bounds.getWidth(), heightOffSet);
            graphics.drawLine((int) bounds.getX()-2, heightOffSet, (int) bounds.getX()-2 + (int) bounds.getWidth(), heightOffSet);
        }
    }

    private GearTagSettings getTag(int itemId) {
        GearTagSettings gearTagSettings = tagCache.getIfPresent(itemId);
        if (gearTagSettings == null)
        {
            gearTagSettings = plugin.getTag(itemId);
            if (gearTagSettings == null)
            {
                return null;
            }

            tagCache.put(itemId, gearTagSettings);
        }
        return gearTagSettings;
    }

    private Image getFillImage(Color color, int itemId, int qty)
    {
        long key = (((long) itemId) << 32) | qty;
        Image image = fillCache.getIfPresent(key);
        if (image == null)
        {
            final Color fillColor = ColorUtil.colorWithAlpha(color, config.fillOpacity());
            image = ImageUtil.fillImage(itemManager.getImage(itemId, qty, false), fillColor);
            fillCache.put(key, image);
        }
        return image;
    }

    void invalidateCache()
    {
        fillCache.invalidateAll();
        tagCache.invalidateAll();
    }
}