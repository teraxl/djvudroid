package org.djvudroid;

import android.app.Dialog;
import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class GoToPageDialog extends Dialog
{
    private final DjvuDocumentView djvuDocumentView;
    private final DecodeService decodeService;

    public GoToPageDialog(final Context context, final DjvuDocumentView djvuDocumentView, final DecodeService decodeService)
    {
        super(context);
        this.djvuDocumentView = djvuDocumentView;
        this.decodeService = decodeService;
        setTitle("Go to page");
        setContentView(R.layout.gotopage);
        final Button button = (Button) findViewById(R.id.goToButton);
        button.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                goToPageAndDismiss();
            }
        });
        final EditText editText = (EditText) findViewById(R.id.pageNumberTextEdit);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent)
            {
                if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_DONE)
                {
                    goToPageAndDismiss();
                    return true;
                }
                return false;
            }
        });
    }

    private void goToPageAndDismiss()
    {
        navigateToPage();
        dismiss();
    }

    private void navigateToPage()
    {
        final EditText text = (EditText) findViewById(R.id.pageNumberTextEdit);
        final int pageNumber = Integer.parseInt(text.getText().toString());
        if (pageNumber < 1 || pageNumber > decodeService.getPageCount())
        {
            Toast.makeText(getContext(), "Page number out of range. Valid range: 1-" + decodeService.getPageCount(), 2000).show();
            return;
        }
        djvuDocumentView.goToPage(pageNumber-1);
    }
}
