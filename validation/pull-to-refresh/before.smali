=== 1. Lil2;->start(I,I)Z ===
// Class: Lil2;
// Method: start(I,I)Z
// Dex: classes.dex
  const/4          const/4 v0, 1
  if-ne            if-ne v8, v0, +012h
  iget-boolean     iget-boolean v1, v7, Lil2;->E Z
  if-nez           if-nez v1, +0e4h
  const-string     const-string v1, "brave_pull_to_refresh"
  sget-object      sget-object v2, Lcp4;->a Landroid/content/SharedPreferences;
  invoke-interface invoke-interface v2, v1, v0, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String; Z)Z
  move-result      move-result v1
  if-nez           if-nez v1, +004h
  goto/16          goto/16 +0d8h
  iget-object      iget-object v1, v7, Lil2;->y Lorg/chromium/chrome/browser/tab/Tab;
  iput             iput v8, v7, Lil2;->v I
  const/4          const/4 v2, 0
  if-ne            if-ne v8, v0, +0bch
  iget-object      iget-object v8, v7, Lil2;->x Lmbi;
  if-nez           if-nez v8, +090h
  invoke-interface invoke-interface v1, Lorg/chromium/chrome/browser/tab/Tab;->p()Landroid/content/Context;
  move-result-object move-result-object v8
  iget-object      iget-object v9, v7, Lil2;->w Lebi;
  invoke-virtual   invoke-virtual v9, Ljava/lang/Object;->getClass()Ljava/lang/Class;
  new-instance     new-instance v9, Lmbi;
  invoke-direct    invoke-direct v9, v8, Lmbi;-><init>(Landroid/content/Context;)V
  iput-object      iput-object v9, v7, Lil2;->x Lmbi;
  new-instance     new-instance v3, Landroid/view/ViewGroup$LayoutParams;
  const/4          const/4 v4, -1
  invoke-direct    invoke-direct v3, v4, v4, Landroid/view/ViewGroup$LayoutParams;-><init>(I I)V
  invoke-virtual   invoke-virtual v9, v3, Landroid/view/View;->setLayoutParams(Landroid/view/ViewGroup$LayoutParams;)V
  invoke-interface invoke-interface v1, Lorg/chromium/chrome/browser/tab/Tab;->e()Z
  move-result      move-result v9
  const-string     const-string v3, "SemanticColorUtils"
  if-eqz           if-eqz v9, +00ah
  const            const v4, 2131165973
  invoke-virtual   invoke-virtual v8, v4, Landroid/content/Context;->getColor(I)I
  move-result      move-result v4
  goto             goto +ch
  const            const v4, 2131034515
  invoke-static    invoke-static v8, v4, v3, Liva;->e(Landroid/content/Context; I Ljava/lang/String;)Landroid/util/TypedValue;
  move-result-object move-result-object v4
  invoke-static    invoke-static v8, v4, Lmwa;->f(Landroid/content/Context; Landroid/util/TypedValue;)I
  move-result      move-result v4
  iget-object      iget-object v5, v7, Lil2;->x Lmbi;
  iget-object      iget-object v6, v5, Lmbi;->C Lww3;
  invoke-virtual   invoke-virtual v6, v4, Lww3;->setBackgroundColor(I)V
  iget-object      iget-object v5, v5, Lmbi;->G Lwwa;
  iget-object      iget-object v5, v5, Lwwa;->v Lvwa;
  iput             iput v4, v5, Lvwa;->w I
  if-eqz           if-eqz v9, +00ah
  const            const v9, 2131165629
  invoke-virtual   invoke-virtual v8, v9, Landroid/content/Context;->getColor(I)I
  move-result      move-result v9
  goto             goto +ch
  const            const v9, 2131034497
  invoke-static    invoke-static v8, v9, v3, Liva;->e(Landroid/content/Context; I Ljava/lang/String;)Landroid/util/TypedValue;
  move-result-object move-result-object v9
  invoke-static    invoke-static v8, v9, Lmwa;->f(Landroid/content/Context; Landroid/util/TypedValue;)I
  move-result      move-result v9
  iget-object      iget-object v3, v7, Lil2;->x Lmbi;
  filled-new-array filled-new-array v9, [I
  move-result-object move-result-object v9
  iget-object      iget-object v3, v3, Lmbi;->G Lwwa;
  iget-object      iget-object v3, v3, Lwwa;->v Lvwa;
  iput-object      iput-object v9, v3, Lvwa;->j [I
  invoke-virtual   invoke-virtual v3, v2, Lvwa;->b(I)V
  invoke-virtual   invoke-virtual v3, v2, Lvwa;->b(I)V
  invoke-interface invoke-interface v1, Lorg/chromium/chrome/browser/tab/Tab;->n()Lpn4;
  move-result-object move-result-object v9
  if-eqz           if-eqz v9, +007h
  iget-object      iget-object v9, v7, Lil2;->x Lmbi;
  invoke-virtual   invoke-virtual v9, v0, Landroid/view/View;->setEnabled(Z)V
  iget-object      iget-object v9, v7, Lil2;->x Lmbi;
  invoke-virtual   invoke-virtual v9, v0, Landroid/view/View;->setAccessibilityLiveRegion(I)V
  iget-object      iget-object v9, v7, Lil2;->x Lmbi;
  new-instance     new-instance v0, Lgbi;
  invoke-direct    invoke-direct v0, Ljava/lang/Object;-><init>()V
  iput-object      iput-object v7, v0, Lgbi;->a Lil2;
  iput-object      iput-object v8, v0, Lgbi;->b Landroid/content/Context;
  iput-object      iput-object v0, v9, Lmbi;->u Llbi;
  new-instance     new-instance v8, Lhbi;
  invoke-direct    invoke-direct v8, Ljava/lang/Object;-><init>()V
  iput-object      iput-object v7, v8, Lhbi;->a Lil2;
  iput-object      iput-object v8, v9, Lmbi;->v Lhbi;
  iget-object      iget-object v8, v7, Lil2;->A Libi;
  if-eqz           if-eqz v8, +00eh
  invoke-static    invoke-static Lorg/chromium/base/ThreadUtils;->b()Landroid/os/Handler;
  move-result-object move-result-object v8
  iget-object      iget-object v9, v7, Lil2;->A Libi;
  invoke-virtual   invoke-virtual v8, v9, Landroid/os/Handler;->removeCallbacks(Ljava/lang/Runnable;)V
  const/4          const/4 v8, 0
  iput-object      iput-object v8, v7, Lil2;->A Libi;
  iget-object      iget-object v8, v7, Lil2;->x Lmbi;
  invoke-virtual   invoke-virtual v8, Landroid/view/View;->getParent()Landroid/view/ViewParent;
  move-result-object move-result-object v8
  if-nez           if-nez v8, +00bh
  invoke-interface invoke-interface v1, Lorg/chromium/chrome/browser/tab/Tab;->n()Lpn4;
  move-result-object move-result-object v8
  iget-object      iget-object v9, v7, Lil2;->x Lmbi;
  invoke-virtual   invoke-virtual v8, v9, Landroid/view/ViewGroup;->addView(Landroid/view/View;)V
  iget-object      iget-object v7, v7, Lil2;->x Lmbi;
  invoke-virtual   invoke-virtual v7, Lmbi;->g()Z
  move-result      move-result v7
  return           return v7
  const/4          const/4 v1, 2
  if-ne            if-ne v8, v1, +016h
  iget-object      iget-object v8, v7, Lil2;->C Lng8;
  if-eqz           if-eqz v8, +044h
  iget-object      iget-object v7, v8, Lng8;->H Lq7c;
  if-eqz           if-eqz v7, +004h
  iput             iput v0, v7, Lq7c;->D I
  if-eqz           if-eqz v7, +009h
  invoke-virtual   invoke-virtual v7, v9, v1, Lq7c;->i(I I)Z
  move-result      move-result v7
  if-eqz           if-eqz v7, +003h
  return           return v0
  return           return v2
  const/4          const/4 v9, 3
  if-ne            if-ne v8, v9, +031h
  iget-object      iget-object v8, v7, Lil2;->D Lpy1;
  if-eqz           if-eqz v8, +02dh
  iget-object      iget-object v7, v8, Lpy1;->a Lez2;
  invoke-virtual   invoke-virtual v7, Lez2;->x()Z
  move-result      move-result v1
  if-eqz           if-eqz v1, +004h
  move             move v0, v2
  goto             goto +18h
  iget-object      iget-object v1, v7, Lez2;->v Ld03;
  invoke-virtual   invoke-virtual v1, Llz2;->s()Ljava/lang/Integer;
  move-result-object move-result-object v1
  invoke-virtual   invoke-virtual v1, Ljava/lang/Integer;->intValue()I
  move-result      move-result v1
  if-eq            if-eq v1, v9, +003h
  goto             goto -eh
  iget             iget v9, v7, Lez2;->H I
  if-nez           if-nez v9, +008h
  invoke-virtual   invoke-virtual v7, Lez2;->r()I
  move-result      move-result v7
  if-eqz           if-eqz v7, -017h
  iput-boolean     iput-boolean v0, v8, Lpy1;->d Z
  const-string     const-string v7, "Android.OverscrollFromBottom.CanStart"
  invoke-static    invoke-static v7, v0, Ljff;->c(Ljava/lang/String; Z)V
  iget-boolean     iget-boolean v7, v8, Lpy1;->d Z
  return           return v7
  iput             iput v2, v7, Lil2;->v I
  return           return v2

=== 2. Lil2;->pull(F,F)V ===
// Class: Lil2;
// Method: pull(F,F)V
// Dex: classes.dex
  const/4          const/4 v0, 0
  const-string     const-string v1, "SwipeRefreshHandler.pull"
  invoke-static    invoke-static v1, v0, Lorg/chromium/base/TraceEvent;->a(Ljava/lang/String; Ljava/lang/String;)V
  iget-object      iget-object v0, v4, Lil2;->x Lmbi;
  iget             iget v2, v4, Lil2;->v I
  const/4          const/4 v3, 1
  if-ne            if-ne v2, v3, +006h
  invoke-virtual   invoke-virtual v0, v6, Lmbi;->b(F)V
  goto             goto +1ah
  const/4          const/4 v0, 2
  if-ne            if-ne v2, v0, +00eh
  iget-object      iget-object v4, v4, Lil2;->C Lng8;
  if-eqz           if-eqz v4, +014h
  iget-object      iget-object v4, v4, Lng8;->H Lq7c;
  if-eqz           if-eqz v4, +010h
  invoke-virtual   invoke-virtual v4, v5, v6, Lq7c;->e(F F)V
  goto             goto +bh
  const/4          const/4 v5, 3
  if-ne            if-ne v2, v5, +009h
  iget-object      iget-object v4, v4, Lil2;->D Lpy1;
  if-eqz           if-eqz v4, +005h
  invoke-virtual   invoke-virtual v4, Ljava/lang/Object;->getClass()Ljava/lang/Class;
  invoke-static    invoke-static v1, Lorg/chromium/base/TraceEvent;->b(Ljava/lang/String;)V
  return-void      return-void 