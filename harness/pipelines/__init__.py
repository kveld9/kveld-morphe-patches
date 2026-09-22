"""
Target Pipelines for Morphe Patches Harness.
Automatically registers all target pipelines into PipelineRegistry.
"""

from harness.pipelines.brave import BravePipeline
from harness.pipelines.gboard import GboardPipeline
from harness.pipelines.vivaldi import VivaldiPipeline
from harness.pipelines.tiktok import TikTokPipeline
from harness.pipelines.xiaomi import XiaomiEarbudsPipeline

__all__ = [
    "BravePipeline",
    "GboardPipeline",
    "VivaldiPipeline",
    "TikTokPipeline",
    "XiaomiEarbudsPipeline",
]

