# In-process Android View overlay for repackaged Unity APKs.
# Installed as smali by tools/inject_apk.py; does not require EGL hooks.
.class public Lcom/jev/inject/JevMenu;
.super Ljava/lang/Object;

.field private static button:Landroid/widget/Button;

.method public static attach(Landroid/app/Activity;)V
    .locals 7
    if-eqz p0, :done
    sget-object v0, Lcom/jev/inject/JevMenu;->button:Landroid/widget/Button;
    if-nez v0, :done

    new-instance v0, Landroid/widget/Button;
    invoke-direct {v0, p0}, Landroid/widget/Button;-><init>(Landroid/content/Context;)V
    const-string v1, "JEV"
    invoke-virtual {v0, v1}, Landroid/widget/Button;->setText(Ljava/lang/CharSequence;)V

    new-instance v1, Lcom/jev/inject/JevMenu$Click;
    invoke-direct {v1, p0}, Lcom/jev/inject/JevMenu$Click;-><init>(Landroid/app/Activity;)V
    invoke-virtual {v0, v1}, Landroid/widget/Button;->setOnClickListener(Landroid/view/View$OnClickListener;)V

    new-instance v1, Landroid/widget/FrameLayout$LayoutParams;
    const/4 v2, -0x2
    invoke-direct {v1, v2, v2}, Landroid/widget/FrameLayout$LayoutParams;-><init>(II)V
    const/16 v2, 0x35
    iput v2, v1, Landroid/widget/FrameLayout$LayoutParams;->gravity:I

    const v2, 0x1020002
    invoke-virtual {p0, v2}, Landroid/app/Activity;->findViewById(I)Landroid/view/View;
    move-result-object v2
    instance-of v3, v2, Landroid/view/ViewGroup;
    if-eqz v3, :done
    check-cast v2, Landroid/view/ViewGroup;
    invoke-virtual {v2, v0, v1}, Landroid/view/ViewGroup;->addView(Landroid/view/View;Landroid/view/ViewGroup$LayoutParams;)V
    sput-object v0, Lcom/jev/inject/JevMenu;->button:Landroid/widget/Button;
:done
    return-void
.end method

.method public static inspect()Ljava/lang/String;
    .locals 1
    invoke-static {}, Lcom/jev/toolkit/JevBridge;->inspect()Ljava/lang/String;
    move-result-object v0
    return-object v0
.end method
