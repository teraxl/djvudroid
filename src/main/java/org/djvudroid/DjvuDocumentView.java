package org.djvudroid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.*;
import com.lizardtech.djvu.GRect;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DjvuDocumentView extends ScrollView
{
    private DecodeService decodeService;
    private final Map<Integer, FrameLayout> pages = new HashMap<Integer, FrameLayout>();
    private final Map<Integer, Bitmap> visiblePageNumToBitmap = new HashMap<Integer, Bitmap>();
    private final Set<Integer> decodingPageNums = new HashSet<Integer>();
    private boolean isInitialized = false;
    private int savedPage;

    public DjvuDocumentView(Context context)
    {
        super(context);
    }

    public void setDecodeService(DecodeService decodeService)
    {
        this.decodeService = decodeService;
    }

    private void init()
    {
        if (isInitialized)
        {
            return;
        }
        final LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        for (int i = 0; i < decodeService.getPageCount(); i++)
        {
            final FrameLayout frameLayout = new FrameLayout(getContext());
            final GRect rect = decodeService.getTargetRect();
            frameLayout.setLayoutParams(new LayoutParams(rect.width(), rect.height()));
            frameLayout.addView(createPageNumView(i));
            pages.put(i, frameLayout);
            linearLayout.addView(frameLayout);
        }
        addView(linearLayout);
        post(new Runnable()
        {
            public void run()
            {
                scrollTo(0, pages.get(savedPage).getTop());
            }
        });
        isInitialized = true;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt)
    {
        super.onScrollChanged(l, t, oldl, oldt);
        for (Integer decodingPageNum : new HashSet<Integer>(decodingPageNums))
        {
            if (!isPageVisible(pages.get(decodingPageNum)))
            {
                decodeService.stopDecoding(decodingPageNum);
                removeDecodingStatus(decodingPageNum);
            }
        }
        for (Integer visiblePageNum : new HashMap<Integer, Bitmap>(visiblePageNumToBitmap).keySet())
        {
            if (!isPageVisible(pages.get(visiblePageNum)))
            {
                removeImageFromPage(visiblePageNum);
            }
        }
        for (final Map.Entry<Integer, FrameLayout> pageNumToPage : pages.entrySet())
        {
            final FrameLayout page = pageNumToPage.getValue();
            if (isPageVisible(page))
            {
                final Integer pageNum = pageNumToPage.getKey();
                if (visiblePageNumToBitmap.containsKey(pageNum))
                {
                    continue;
                }
                decodePage(pageNum);
            }
        }
    }

    private void decodePage(final Integer pageNum)
    {
        if (decodingPageNums.contains(pageNum))
        {
            return;
        }
        addDecodingStatus(pageNum);
        decodeService.decodePage(pageNum, new DecodeService.DecodeCallback()
        {
            public void decodeComplete(final Bitmap bitmap)
            {
                post(new Runnable()
                {
                    public void run()
                    {
                        submitBitmap(pageNum, bitmap);
                    }
                });
            }
        });
    }

    private void addDecodingStatus(Integer pageNum)
    {
        if (!decodingPageNums.contains(pageNum) && isInitialized)
        {
            final ProgressBar bar = new ProgressBar(getContext());
            bar.setIndeterminate(true);
            bar.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
            bar.setTag(ProgressBar.class);
            pages.get(pageNum).addView(bar);
        }
        decodingPageNums.add(pageNum);
    }

    private void removeDecodingStatus(Integer decodingPageNum)
    {
        if (decodingPageNums.contains(decodingPageNum) && isInitialized)
        {
            final FrameLayout page = pages.get(decodingPageNum);
            page.removeView(page.findViewWithTag(ProgressBar.class));
        }
        decodingPageNums.remove(decodingPageNum);
    }

    private boolean isPageVisible(FrameLayout page)
    {
        return page.getGlobalVisibleRect(new Rect());
    }

    private void submitBitmap(Integer pageNum, Bitmap bitmap)
    {
        addImageToPage(pageNum, bitmap);
        removeDecodingStatus(pageNum);
    }

    private void addImageToPage(Integer pageNum, Bitmap bitmap)
    {
        init();
        final ImageView imageView = createImageView(bitmap);
        final FrameLayout page = pages.get(pageNum);
        page.addView(imageView);
        page.setLayoutParams(new LinearLayout.LayoutParams(bitmap.getWidth(), bitmap.getHeight()));
        visiblePageNumToBitmap.put(pageNum, bitmap);
    }

    private void removeImageFromPage(Integer fromPage)
    {
        final FrameLayout page = pages.get(fromPage);
        page.removeView(page.findViewWithTag(ImageView.class));
        final Bitmap bitmap = visiblePageNumToBitmap.remove(fromPage);
        decodeService.freeBitmap(bitmap);
    }

    private ImageView createImageView(Bitmap bitmap)
    {
        final ImageView imageView = new ImageView(getContext());
        imageView.setImageBitmap(bitmap);
        imageView.setTag(ImageView.class);
        return imageView;
    }

    private TextView createPageNumView(int i)
    {
        TextView pageNumTextView = new TextView(getContext());
        pageNumTextView.setText("Page " + (i+1));
        pageNumTextView.setTextSize(32);
        pageNumTextView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        pageNumTextView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        return pageNumTextView;
    }

    public void showDocument()
    {
        decodePage(0);
    }

    public void setSavedPage(int savedPage)
    {
        this.savedPage = savedPage;
    }

    public int getPageToSave()
    {
        for (Map.Entry<Integer, FrameLayout> entry : pages.entrySet())
        {
            if (isPageVisible(entry.getValue()))
            {
                return entry.getKey();
            }
        }
        return 0;
    }
}
