'use client';

import { useState, useEffect, useRef, useLayoutEffect } from 'react';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { ScrollingText, ScrollDirection } from '@/components/ui/scrolling-text';

/** Nominal layout width (px) for calibrating scroll speed in the dialog preview */
const TEXT_PREVIEW_REFERENCE_LAYOUT_W = 1920;

export interface TextConfig {
  text: string;
  direction: ScrollDirection;
  speed: number;
  fontSize: number;
  fontWeight: 'normal' | 'bold' | 'bolder' | 'lighter';
  textColor: string;
  backgroundColor: string;
}

interface TextConfigDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: (config: TextConfig) => void;
  initialConfig?: Partial<TextConfig>;
  sectionName?: string;
}

export function TextConfigDialog({
  open,
  onOpenChange,
  onConfirm,
  initialConfig = {},
  sectionName = 'Text Section',
}: TextConfigDialogProps) {
  const previewBoxRef = useRef<HTMLDivElement>(null);
  const [previewBoxWidth, setPreviewBoxWidth] = useState(0);

  const [config, setConfig] = useState<TextConfig>({
    text: '',
    direction: 'left-to-right',
    speed: 50,
    fontSize: 24,
    fontWeight: 'normal',
    textColor: '#000000',
    backgroundColor: 'transparent',
    ...initialConfig,
  });

  useLayoutEffect(() => {
    if (!open || !previewBoxRef.current) return;
    setPreviewBoxWidth(previewBoxRef.current.getBoundingClientRect().width);
  }, [open]);

  useEffect(() => {
    if (!open || !previewBoxRef.current) return;
    const el = previewBoxRef.current;
    const ro = new ResizeObserver(() => setPreviewBoxWidth(el.getBoundingClientRect().width));
    ro.observe(el);
    return () => ro.disconnect();
  }, [open]);

  useEffect(() => {
    if (open) {
      setConfig({
        text: '',
        direction: 'left-to-right',
        speed: 50,
        fontSize: 24,
        fontWeight: 'normal',
        textColor: '#000000',
        backgroundColor: 'transparent',
        ...initialConfig,
      });
    }
  }, [open, initialConfig]);

  const textPreviewScale =
    previewBoxWidth > 0 ? previewBoxWidth / TEXT_PREVIEW_REFERENCE_LAYOUT_W : 0.35;

  const handleConfirm = () => {
    if (!config.text.trim()) {
      return; // Visual feedback is already provided by the red border
    }
    onConfirm(config);
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Configure Scrolling Text</DialogTitle>
          <DialogDescription>
            Set up scrolling text for &quot;{sectionName}&quot;
          </DialogDescription>
        </DialogHeader>
        
        <div className="space-y-6 py-4">
          {/* Text Input */}
          <div className="space-y-2">
            <Label htmlFor="text" className="flex items-center gap-1">
              Text to Display 
              <span className="text-red-500">*</span>
              {!config.text.trim() && (
                <span className="text-xs text-red-500 ml-2">(Required)</span>
              )}
            </Label>
            <textarea
              id="text"
              value={config.text}
              onChange={(e) => setConfig({ ...config, text: e.target.value })}
              placeholder="Enter your scrolling text here..."
              className={`w-full rounded-md border px-3 py-2 text-base min-h-[80px] resize-vertical focus:outline-none focus:ring-1 ${
                !config.text.trim() 
                  ? 'border-red-300 focus:border-red-500 focus:ring-red-500' 
                  : 'border-gray-300 focus:border-blue-500 focus:ring-blue-500'
              }`}
              rows={3}
            />
          </div>

          {/* Direction and Speed */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="direction">Scroll Direction</Label>
              <select
                id="direction"
                value={config.direction}
                onChange={(e) => setConfig({ ...config, direction: e.target.value as ScrollDirection })}
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
              >
                <option value="left-to-right">Left to Right →</option>
                <option value="right-to-left">Right to Left ←</option>
                <option value="top-to-bottom">Top to Bottom ↓</option>
                <option value="bottom-to-top">Bottom to Top ↑</option>
              </select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="speed">Speed (px/sec)</Label>
              <Input
                id="speed"
                type="number"
                value={config.speed}
                onChange={(e) => setConfig({ ...config, speed: Number(e.target.value) })}
                min="10"
                max="200"
                step="5"
              />
            </div>
          </div>

          {/* Font Settings */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="fontSize">Font Size (px)</Label>
              <Input
                id="fontSize"
                type="number"
                value={config.fontSize}
                onChange={(e) => setConfig({ ...config, fontSize: Number(e.target.value) })}
                min="12"
                max="100"
                step="2"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="fontWeight">Font Weight</Label>
              <select
                id="fontWeight"
                value={config.fontWeight}
                onChange={(e) => setConfig({ ...config, fontWeight: e.target.value as any })}
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
              >
                <option value="lighter">Lighter</option>
                <option value="normal">Normal</option>
                <option value="bold">Bold</option>
                <option value="bolder">Bolder</option>
              </select>
            </div>
          </div>

          {/* Colors */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="textColor">Text Color</Label>
              <div className="flex gap-2">
                <Input
                  id="textColor"
                  type="color"
                  value={config.textColor}
                  onChange={(e) => setConfig({ ...config, textColor: e.target.value })}
                  className="w-16 h-10 p-1 border rounded"
                />
                <Input
                  value={config.textColor}
                  onChange={(e) => setConfig({ ...config, textColor: e.target.value })}
                  placeholder="#000000"
                  className="flex-1"
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="backgroundColor">Background Color</Label>
              <div className="flex gap-2">
                <Input
                  id="backgroundColor"
                  type="color"
                  value={config.backgroundColor === 'transparent' ? '#ffffff' : config.backgroundColor}
                  onChange={(e) => setConfig({ ...config, backgroundColor: e.target.value })}
                  className="w-16 h-10 p-1 border rounded"
                />
                <select
                  value={config.backgroundColor === 'transparent' ? 'transparent' : 'color'}
                  onChange={(e) => setConfig({ 
                    ...config, 
                    backgroundColor: e.target.value === 'transparent' ? 'transparent' : '#ffffff'
                  })}
                  className="flex-1 rounded-md border border-gray-300 px-3 py-2 text-sm"
                >
                  <option value="transparent">Transparent</option>
                  <option value="color">Custom Color</option>
                </select>
              </div>
            </div>
          </div>

          {/* Preview */}
          <div className="space-y-2">
            <Label>Preview</Label>
            <div
              ref={previewBoxRef}
              className="border-2 border-gray-200 rounded-lg overflow-hidden bg-gray-100"
              style={{ height: '120px' }}
            >
              <ScrollingText
                text={config.text || 'Sample scrolling text preview'}
                direction={config.direction}
                speed={config.speed}
                fontSize={Math.max(8, config.fontSize * textPreviewScale)}
                fontWeight={config.fontWeight}
                textColor={config.textColor}
                backgroundColor={config.backgroundColor}
                className="w-full h-full"
              />
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button 
            onClick={handleConfirm}
            disabled={!config.text.trim()}
            className={!config.text.trim() ? 'opacity-50 cursor-not-allowed' : ''}
          >
            Apply Text Configuration
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}