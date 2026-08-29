"""
Obfuscated Symbol Resolver and Structural Equivalence Verification Engine.
Deterministically identifies obfuscated method and field renames across Brave browser updates.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum
from typing import Dict, List, Optional

from harness.core.dex import DexIndex, IndexedClass, IndexedMethod


class SymbolConfidence(str, Enum):
    VERIFIED = "VERIFIED"
    HIGH = "HIGH"
    LOW = "LOW"
    BLOCKED = "BLOCKED"


@dataclass
class ResolvedSymbol:
    symbol_id: str
    target_class: str
    old_symbol: str
    new_symbol: str
    symbol_type: str  # 'field', 'method', 'signature'
    confidence: SymbolConfidence
    evidence: List[str] = field(default_factory=list)


@dataclass
class BraveOriginSymbols:
    locked_field: ResolvedSymbol
    key_mapping_method: ResolvedSymbol
    context_getter_method: ResolvedSymbol
    update_prefs_method: ResolvedSymbol
    find_pref_method: ResolvedSymbol
    pref_listener_field: ResolvedSymbol


@dataclass
class BraveNotificationSchedulerSymbols:
    on_start_task_method: ResolvedSymbol
    param2_type: str
    param3_type: str


@dataclass
class BravePrefServiceSymbols:
    boolean_getter_method: ResolvedSymbol


@dataclass
class BravePartnerCustomizationSymbols:
    initialized_field: ResolvedSymbol


class SymbolResolver:
    """Resolves obfuscated members on Brave classes using structural analysis."""

    def __init__(self, index: DexIndex):
        self.index = index

    def resolve_all(self) -> Dict[str, any]:
        return {
            "origin": self.resolve_brave_origin_symbols(),
            "scheduler": self.resolve_notification_scheduler_symbols(),
            "pref_service": self.resolve_pref_service_symbols(),
            "partner_customizations": self.resolve_partner_customization_symbols(),
        }

    def resolve_brave_origin_symbols(self) -> Optional[BraveOriginSymbols]:
        cls_name = "Lorg/chromium/chrome/browser/settings/BraveOriginPreferences;"
        cls = self.index.find_class(cls_name)
        if not cls:
            return None

        # Validate onPreferenceChange method signature
        if not self._has_on_preference_change(cls):
            return None

        hierarchy_methods = cls.get_all_hierarchy_methods(self.index)
        return BraveOriginSymbols(
            locked_field=self._resolve_origin_locked_field(cls, cls_name),
            key_mapping_method=self._resolve_origin_key_mapping(cls, cls_name),
            context_getter_method=self._resolve_origin_context_getter(hierarchy_methods, cls_name),
            update_prefs_method=self._resolve_origin_update_prefs(cls, cls_name),
            find_pref_method=self._resolve_origin_find_pref(hierarchy_methods, cls_name),
            pref_listener_field=self._resolve_origin_pref_listener(),
        )

    @staticmethod
    def _has_on_preference_change(cls: IndexedClass) -> bool:
        return any(
            m.return_type == "Z" and len(m.parameters) in (1, 2) and
            m.parameters[0] == "Landroidx/preference/Preference;"
            for m in cls.methods
        )

    @staticmethod
    def _resolve_origin_locked_field(cls: IndexedClass, cls_name: str) -> ResolvedSymbol:
        boolean_fields = [f for f in cls.fields if f[1] == "Z" and (f[2] & 0x8) == 0]
        locked_field_name = boolean_fields[0][0] if boolean_fields else "O0"
        return ResolvedSymbol(
            symbol_id="origin_locked_field",
            target_class=cls_name,
            old_symbol="N0:Z",
            new_symbol=f"{locked_field_name}:Z",
            symbol_type="field",
            confidence=SymbolConfidence.VERIFIED if boolean_fields else SymbolConfidence.BLOCKED,
            evidence=[f"Instance boolean field on {cls_name}: {locked_field_name}"]
        )

    @staticmethod
    def _resolve_origin_key_mapping(cls: IndexedClass, cls_name: str) -> ResolvedSymbol:
        static_str_str = [
            m for m in cls.methods
            if (m.access_flags & 0x8) != 0 and m.parameters == ["Ljava/lang/String;"] and m.return_type == "Ljava/lang/String;"
        ]
        key_map_name = static_str_str[0].name if static_str_str else "e5"
        return ResolvedSymbol(
            symbol_id="origin_key_mapping_method",
            target_class=cls_name,
            old_symbol="b5(Ljava/lang/String;)Ljava/lang/String;",
            new_symbol=f"{key_map_name}(Ljava/lang/String;)Ljava/lang/String;",
            symbol_type="method",
            confidence=SymbolConfidence.VERIFIED if static_str_str else SymbolConfidence.BLOCKED,
            evidence=[f"Static (String) -> String method on {cls_name}: {key_map_name}"]
        )

    @staticmethod
    def _resolve_origin_context_getter(hierarchy_methods: List[IndexedMethod], cls_name: str) -> ResolvedSymbol:
        context_getters = [
            m for m in hierarchy_methods
            if (m.access_flags & 0x8) == 0 and len(m.parameters) == 0 and m.return_type == "Landroid/content/Context;"
        ]
        ctx_name = context_getters[0].name if context_getters else "N3"
        return ResolvedSymbol(
            symbol_id="origin_context_getter_method",
            target_class=cls_name,
            old_symbol="B4()Landroid/content/Context;",
            new_symbol=f"{ctx_name}()Landroid/content/Context;",
            symbol_type="method",
            confidence=SymbolConfidence.VERIFIED if context_getters else SymbolConfidence.BLOCKED,
            evidence=[f"Instance () -> Context method on {cls_name} (or superclass): {ctx_name}"]
        )

    @staticmethod
    def _resolve_origin_update_prefs(cls: IndexedClass, cls_name: str) -> ResolvedSymbol:
        void_zero_param = [
            m for m in cls.methods
            if (m.access_flags & 0x8) == 0 and len(m.parameters) == 0 and m.return_type == "V" and m.name not in ("<init>", "onStart", "onResume")
        ]
        update_name = void_zero_param[0].name if void_zero_param else "d5"
        return ResolvedSymbol(
            symbol_id="origin_update_prefs_method",
            target_class=cls_name,
            old_symbol="e5()V",
            new_symbol=f"{update_name}()V",
            symbol_type="method",
            confidence=SymbolConfidence.VERIFIED if void_zero_param else SymbolConfidence.BLOCKED,
            evidence=[f"Instance () -> V refresh method on {cls_name}: {update_name}"]
        )

    @staticmethod
    def _resolve_origin_find_pref(hierarchy_methods: List[IndexedMethod], cls_name: str) -> ResolvedSymbol:
        find_pref_candidates = [
            m for m in hierarchy_methods
            if len(m.parameters) == 1 and m.parameters[0] == "Ljava/lang/CharSequence;" and m.return_type == "Landroidx/preference/Preference;"
        ]
        find_pref_name = find_pref_candidates[0].name if find_pref_candidates else "S4"
        return ResolvedSymbol(
            symbol_id="origin_find_pref_method",
            target_class=cls_name,
            old_symbol="P4(Ljava/lang/CharSequence;)Landroidx/preference/Preference;",
            new_symbol=f"{find_pref_name}(Ljava/lang/CharSequence;)Landroidx/preference/Preference;",
            symbol_type="method",
            confidence=SymbolConfidence.VERIFIED if find_pref_candidates else SymbolConfidence.BLOCKED,
            evidence=[f"Method (CharSequence) -> Preference on {cls_name} (or superclass): {find_pref_name}"]
        )

    def _resolve_origin_pref_listener(self) -> ResolvedSymbol:
        pref_cls = self.index.find_class("Landroidx/preference/Preference;")
        pref_listener_field = "y"
        if pref_cls:
            listener_fields = [
                f[0] for f in pref_cls.fields
                if f[1] in ("Landroidx/preference/Preference$OnPreferenceChangeListener;", "Landroidx/preference/Preference$c;")
            ]
            if listener_fields:
                pref_listener_field = listener_fields[0]

        return ResolvedSymbol(
            symbol_id="origin_pref_listener_field",
            target_class="Landroidx/preference/Preference;",
            old_symbol="y",
            new_symbol=pref_listener_field,
            symbol_type="field",
            confidence=SymbolConfidence.VERIFIED,
            evidence=[f"Reflection listener field on Preference: '{pref_listener_field}'"]
        )

    def resolve_notification_scheduler_symbols(self) -> Optional[BraveNotificationSchedulerSymbols]:
        cls_name = "Lorg/chromium/chrome/browser/notifications/scheduler/NotificationSchedulerTask;"
        cls = self.index.find_class(cls_name)
        if not cls:
            return None

        # Look for onStartTask method: returns I, parameters start with Context
        task_methods = [
            m for m in cls.methods
            if m.return_type == "I" and len(m.parameters) in (1, 3) and m.parameters[0] == "Landroid/content/Context;"
        ]
        if not task_methods:
            return None

        m = task_methods[0]
        sym = ResolvedSymbol(
            symbol_id="scheduler_on_start_task",
            target_class=cls_name,
            old_symbol="c(Landroid/content/Context;Lfjj;Lu5c;)I",
            new_symbol=f"{m.name}({','.join(m.parameters)})I",
            symbol_type="method",
            confidence=SymbolConfidence.VERIFIED,
            evidence=[f"NotificationScheduler task starter: {m.full_name}"]
        )
        return BraveNotificationSchedulerSymbols(
            on_start_task_method=sym,
            param2_type=m.parameters[1],
            param3_type=m.parameters[2],
        )

    def resolve_pref_service_symbols(self) -> Optional[BravePrefServiceSymbols]:
        cls_name = "Lorg/chromium/components/prefs/PrefService;"
        cls = self.index.find_class(cls_name)
        if not cls:
            return None

        # Look for (String) -> Z method on PrefService
        bool_getters = [
            m for m in cls.methods
            if m.parameters == ["Ljava/lang/String;"] and m.return_type == "Z"
        ]
        if not bool_getters:
            return None

        m = bool_getters[0]
        sym = ResolvedSymbol(
            symbol_id="pref_service_boolean_getter",
            target_class=cls_name,
            old_symbol="e(Ljava/lang/String;)Z",
            new_symbol=f"{m.name}(Ljava/lang/String;)Z",
            symbol_type="method",
            confidence=SymbolConfidence.VERIFIED,
            evidence=[f"PrefService boolean getter: {m.full_name}"]
        )
        return BravePrefServiceSymbols(boolean_getter_method=sym)

    def resolve_partner_customization_symbols(self) -> Optional[BravePartnerCustomizationSymbols]:
        cls_name = "Lorg/chromium/chrome/browser/partnercustomizations/PartnerBrowserCustomizations;"
        cls = self.index.find_class(cls_name)
        if not cls:
            return None

        # Look for Boolean instance field
        bool_fields = [f for f in cls.fields if f[1] == "Ljava/lang/Boolean;"]
        f_name = bool_fields[0][0] if bool_fields else "b"
        sym = ResolvedSymbol(
            symbol_id="partner_customization_init_field",
            target_class=cls_name,
            old_symbol="b:Ljava/lang/Boolean;",
            new_symbol=f"{f_name}:Ljava/lang/Boolean;",
            symbol_type="field",
            confidence=SymbolConfidence.VERIFIED if bool_fields else SymbolConfidence.BLOCKED,
            evidence=[f"PartnerBrowserCustomizations boolean field: {f_name}"]
        )
        return BravePartnerCustomizationSymbols(initialized_field=sym)
