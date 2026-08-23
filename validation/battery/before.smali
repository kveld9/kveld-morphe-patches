// Class: Lzh1;
// Method: onReceive(Landroid/content/Context;,Landroid/content/Intent;)V
// Dex: classes2.dex
  iget-object      iget-object v11, v11, Lzh1;->a Lci1;
  iget-object      iget-object v12, v11, Lci1;->a Lmh1;
  const-string     const-string v0, "android.intent.action.BATTERY_CHANGED"
  invoke-virtual   invoke-virtual v13, Landroid/content/Intent;->getAction()Ljava/lang/String;
  move-result-object move-result-object v1
  invoke-virtual   invoke-virtual v0, v1, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
  move-result      move-result v0
  if-nez           if-nez v0, +00ah
  const-string     const-string v11, "Unexpected intent."
  const-string     const-string v12, "cr_BatteryStatusManager"
  invoke-static    invoke-static v12, v11, Landroid/util/Log;->e(Ljava/lang/String; Ljava/lang/String;)I
  return-void      return-void 
  const-string     const-string v0, "present"
  const/4          const/4 v1, 0
  invoke-virtual   invoke-virtual v13, v0, v1, Landroid/content/Intent;->getBooleanExtra(Ljava/lang/String; Z)Z
  move-result      move-result v0
  const-string     const-string v2, "plugged"
  const/4          const/4 v3, -1
  invoke-virtual   invoke-virtual v13, v2, v3, Landroid/content/Intent;->getIntExtra(Ljava/lang/String; I)I
  move-result      move-result v2
  if-eqz           if-eqz v0, +05ah
  if-ne            if-ne v2, v3, +003h
  goto             goto +56h
  const-string     const-string v0, "level"
  invoke-virtual   invoke-virtual v13, v0, v3, Landroid/content/Intent;->getIntExtra(Ljava/lang/String; I)I
  move-result      move-result v0
  const-string     const-string v4, "scale"
  invoke-virtual   invoke-virtual v13, v4, v3, Landroid/content/Intent;->getIntExtra(Ljava/lang/String; I)I
  move-result      move-result v4
  int-to-double    int-to-double v5, v0
  int-to-double    int-to-double v7, v4
  div-double/2addr div-double/2addr v5, v7
  const-wide/16    const-wide/16 v7, 0
  cmpg-double      cmpg-double v0, v5, v7
  const-wide/high16 const-wide/high16 v9, 4607182418800017408
  if-ltz           if-ltz v0, +006h
  cmpl-double      cmpl-double v0, v5, v9
  if-lez           if-lez v0, +003h
  move-wide        move-wide v5, v9
  const/4          const/4 v0, 1
  if-eqz           if-eqz v2, +004h
  move             move v2, v0
  goto             goto +2h
  move             move v2, v1
  const-string     const-string v4, "status"
  invoke-virtual   invoke-virtual v13, v4, v3, Landroid/content/Intent;->getIntExtra(Ljava/lang/String; I)I
  move-result      move-result v13
  const/4          const/4 v3, 5
  if-ne            if-ne v13, v3, +003h
  goto             goto +2h
  move             move v0, v1
  const-wide/high16 const-wide/high16 v3, 9218868437227405312
  if-eqz           if-eqz v2, +005h
  if-eqz           if-eqz v0, +003h
  goto             goto +2h
  move-wide        move-wide v7, v3
  new-instance     new-instance v13, Lxh1;
  invoke-direct    invoke-direct v13, v1, Lxh1;-><init>(I)V
  iput-boolean     iput-boolean v2, v13, Lxh1;->b Z
  iput-wide        iput-wide v7, v13, Lxh1;->c D
  iput-wide        iput-wide v3, v13, Lxh1;->d D
  iput-wide        iput-wide v5, v13, Lxh1;->e D
  iget-object      iget-object v0, v11, Lci1;->d Lbi1;
  if-eqz           if-eqz v0, +00dh
  new-instance     new-instance v12, Lai1;
  invoke-direct    invoke-direct v12, v11, v13, Lai1;-><init>(Lci1; Lxh1;)V
  sget-object      sget-object v11, Lhm0;->e Ldm0;
  invoke-virtual   invoke-virtual v12, v11, Lhm0;->c(Lcja;)V
  return-void      return-void 
  invoke-virtual   invoke-virtual v12, v13, Lmh1;->a(Lxh1;)V
  return-void      return-void 
  new-instance     new-instance v11, Lxh1;
  invoke-direct    invoke-direct v11, v1, Lxh1;-><init>(I)V
  invoke-virtual   invoke-virtual v12, v11, Lmh1;->a(Lxh1;)V
  return-void      return-void 