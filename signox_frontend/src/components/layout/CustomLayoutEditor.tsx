'use client';

import { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';
import { Plus, X, Trash2, Save, Layers, Type, Image as ImageIcon } from 'lucide-react';

type CustomSection = {
  id: string;
  name: string;
  order: number;
  x: number;
  y: number;
  width: number;
  height: number;
  type: 'media' | 'text';
};

type Resolution = {
  label: string;
  width: number;
  height: number;
};

const RESOLUTIONS: Resolution[] = [
  { label: 'HD 720p', width: 1280, height: 720 },
  { label: 'HD 1080p', width: 1920, height: 1080 },
  { label: '2K QHD', width: 2560, height: 1440 },
  { label: '4K UHD', width: 3840, height: 2160 },
];

interface CustomLayoutEditorProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSave: (layoutData: {
    name: string;
    width: number;
    height: number;
    orientation: 'LANDSCAPE' | 'PORTRAIT';
    sections: CustomSection[];
  }) => void;
}

export function CustomLayoutEditor({
  open,
  onOpenChange,
  onSave,
}: CustomLayoutEditorProps) {
  const [layoutName, setLayoutName] = useState('');
  const [resolution, setResolution] = useState<Resolution>(RESOLUTIONS[1]); // Default HD 1080p
  const [orientation, setOrientation] = useState<'LANDSCAPE' | 'PORTRAIT'>('LANDSCAPE');
  const [sections, setSections] = useState<CustomSection[]>([]);
  const [selectedSectionId, setSelectedSectionId] = useState<string | null>(null);
  const [canvasScale, setCanvasScale] = useState(0.3); // Start at 30% zoom
  const [autoScale, setAutoScale] = useState(false); // Don't auto-scale initially
  const [isDrawingMode, setIsDrawingMode] = useState(false);
  const [drawingStart, setDrawingStart] = useState<{x: number, y: number} | null>(null);
  const [drawingRect, setDrawingRect] = useState<{x: number, y: number, width: number, height: number} | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [dragStart, setDragStart] = useState({ x: 0, y: 0 });
  
  // Section naming dialog
  const [namingDialogOpen, setNamingDialogOpen] = useState(false);
  const [pendingSection, setPendingSection] = useState<CustomSection | null>(null);
  const [sectionName, setSectionName] = useState('');
  const [sectionType, setSectionType] = useState<'media' | 'text'>('media');

  const canvasRef = useRef<HTMLDivElement>(null);

  // Calculate canvas scale to always show full canvas, then allow manual zoom
  useEffect(() => {
    if (!canvasRef.current || !resolution || !open) return;

    const updateScale = () => {
      const container = canvasRef.current?.parentElement;
      if (container) {
        // Always calculate the fit-to-container scale as the base
        const containerWidth = container.clientWidth;
        const containerHeight = container.clientHeight;
        const padding = 60; // Account for margins and padding
        
        const maxWidth = containerWidth - padding;
        const maxHeight = containerHeight - padding;
        
        const scaleX = maxWidth / resolution.width;
        const scaleY = maxHeight / resolution.height;
        
        // Use smaller scale to ensure full canvas fits - this is our "fit" scale
        const fitScale = Math.min(scaleX, scaleY, 1.0);
        const baseScale = Math.max(fitScale, 0.3); // Minimum 30% scale
        
        // Only set scale if auto-scale is enabled (initial load)
        if (autoScale) {
          setCanvasScale(baseScale);
          setAutoScale(false); // After initial fit, allow manual control
        }
      }
    };

    updateScale();

    const resizeObserver = new ResizeObserver(updateScale);
    const container = canvasRef.current.parentElement;
    if (container) {
      resizeObserver.observe(container);
    }

    return () => {
      resizeObserver.disconnect();
    };
  }, [resolution, open, autoScale]);

  // Calculate canvas dimensions
  const canvasWidth = useMemo(() => resolution.width * canvasScale, [resolution.width, canvasScale]);
  const canvasHeight = useMemo(() => resolution.height * canvasScale, [resolution.height, canvasScale]);

  const selectedSection = sections.find((s) => s.id === selectedSectionId);

  // Reset state when dialog opens/closes
  useEffect(() => {
    if (open) {
      setLayoutName('Custom Layout');
      setSections([]);
      setSelectedSectionId(null);
      setIsDrawingMode(false);
      setDrawingStart(null);
      setDrawingRect(null);
      setCanvasScale(0.3); // Always start at 30% zoom
      setAutoScale(false); // Don't auto-scale
    }
  }, [open]);

  // Update dimensions when orientation changes
  useEffect(() => {
    if (open) {
      const baseRes = RESOLUTIONS.find((r) => r.label === resolution.label);
      if (baseRes) {
        const newWidth = orientation === 'LANDSCAPE' ? baseRes.width : baseRes.height;
        const newHeight = orientation === 'LANDSCAPE' ? baseRes.height : baseRes.width;
        setResolution({ ...baseRes, width: newWidth, height: newHeight });
      }
    }
  }, [orientation, open]);

  const handleCanvasMouseDown = (e: React.MouseEvent) => {
    if (!isDrawingMode) {
      setSelectedSectionId(null);
      return;
    }
    
    const rect = canvasRef.current?.getBoundingClientRect();
    if (rect) {
      const canvasX = (e.clientX - rect.left) / canvasScale;
      const canvasY = (e.clientY - rect.top) / canvasScale;
      
      setDrawingStart({
        x: (canvasX / resolution.width) * 100,
        y: (canvasY / resolution.height) * 100
      });
    }
  };

  const handleCanvasMouseMove = useCallback((e: MouseEvent) => {
    if (!isDrawingMode || !drawingStart || !canvasRef.current) return;
    
    const rect = canvasRef.current.getBoundingClientRect();
    const canvasX = (e.clientX - rect.left) / canvasScale;
    const canvasY = (e.clientY - rect.top) / canvasScale;
    
    const currentX = (canvasX / resolution.width) * 100;
    const currentY = (canvasY / resolution.height) * 100;
    
    setDrawingRect({
      x: Math.min(drawingStart.x, currentX),
      y: Math.min(drawingStart.y, currentY),
      width: Math.abs(currentX - drawingStart.x),
      height: Math.abs(currentY - drawingStart.y)
    });
  }, [isDrawingMode, drawingStart, canvasScale, resolution]);

  const handleCanvasMouseUp = useCallback(() => {
    if (isDrawingMode && drawingRect && drawingRect.width > 2 && drawingRect.height > 2) {
      // Create new section from drawn rectangle
      const newSection: CustomSection = {
        id: `custom-${Date.now()}`,
        name: '',
        order: sections.length,
        x: Math.max(0, Math.min(100 - drawingRect.width, drawingRect.x)),
        y: Math.max(0, Math.min(100 - drawingRect.height, drawingRect.y)),
        width: Math.min(drawingRect.width, 100),
        height: Math.min(drawingRect.height, 100),
        type: 'media'
      };
      
      setPendingSection(newSection);
      setSectionName(`Section ${sections.length + 1}`);
      setSectionType('media');
      setNamingDialogOpen(true);
    }
    
    setDrawingStart(null);
    setDrawingRect(null);
    setIsDrawingMode(false);
  }, [isDrawingMode, drawingRect, sections.length]);

  // Mouse event listeners and keyboard shortcuts
  useEffect(() => {
    if (isDrawingMode) {
      document.addEventListener('mousemove', handleCanvasMouseMove);
      document.addEventListener('mouseup', handleCanvasMouseUp);
    }

    // Keyboard shortcuts for zoom
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.ctrlKey || e.metaKey) {
        if (e.key === '=' || e.key === '+') {
          e.preventDefault();
          setCanvasScale(prev => Math.min(prev + 0.1, 3.0));
        } else if (e.key === '-') {
          e.preventDefault();
          setCanvasScale(prev => Math.max(prev - 0.1, 0.2));
        } else if (e.key === '0') {
          e.preventDefault();
          // Calculate fit-to-container scale
          const container = canvasRef.current?.parentElement;
          if (container) {
            const containerWidth = container.clientWidth;
            const containerHeight = container.clientHeight;
            const padding = 60;
            
            const maxWidth = containerWidth - padding;
            const maxHeight = containerHeight - padding;
            
            const scaleX = maxWidth / resolution.width;
            const scaleY = maxHeight / resolution.height;
            
            const fitScale = Math.min(scaleX, scaleY, 1.0);
            setCanvasScale(Math.max(fitScale, 0.3));
          }
        }
      }
    };

    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('mousemove', handleCanvasMouseMove);
      document.removeEventListener('mouseup', handleCanvasMouseUp);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isDrawingMode, handleCanvasMouseMove, handleCanvasMouseUp]);

  const confirmSectionCreation = () => {
    if (pendingSection && sectionName.trim()) {
      const finalSection = {
        ...pendingSection,
        name: sectionName.trim(),
        type: sectionType,
        order: sections.length // This will be the z-index for overlaying
      };
      setSections([...sections, finalSection]);
      setSelectedSectionId(finalSection.id);
    }
    setNamingDialogOpen(false);
    setPendingSection(null);
    setSectionName('');
  };

  const cancelSectionCreation = () => {
    setNamingDialogOpen(false);
    setPendingSection(null);
    setSectionName('');
  };

  const removeSection = (sectionId: string) => {
    setSections(sections.filter(s => s.id !== sectionId));
    if (selectedSectionId === sectionId) {
      setSelectedSectionId(null);
    }
  };

  const updateSection = (id: string, updates: Partial<CustomSection>) => {
    setSections(sections.map(s => s.id === id ? { ...s, ...updates } : s));
  };

  const handleSectionClick = (e: React.MouseEvent, section: CustomSection) => {
    e.stopPropagation();
    setSelectedSectionId(section.id);
  };

  const handleSave = () => {
    if (!layoutName.trim()) {
      alert('Please enter a layout name');
      return;
    }
    
    if (sections.length === 0) {
      alert('Please add at least one section to the layout');
      return;
    }

    onSave({
      name: layoutName.trim(),
      width: resolution.width,
      height: resolution.height,
      orientation,
      sections
    });
  };

  return (
    <>
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="max-w-6xl h-[90vh] flex flex-col">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Layers className="h-5 w-5" />
              Create Custom Layout
            </DialogTitle>
          </DialogHeader>
          
          <div className="flex-1 flex gap-6 overflow-hidden">
            {/* Left Panel - Controls */}
            <div className="w-80 space-y-6 overflow-y-auto">
              {/* Layout Settings */}
              <div className="space-y-4">
                <h3 className="font-semibold text-lg">Layout Settings</h3>
                
                <div>
                  <Label>Layout Name</Label>
                  <Input
                    value={layoutName}
                    onChange={(e) => setLayoutName(e.target.value)}
                    placeholder="Enter layout name"
                  />
                </div>

                <div>
                  <Label>Resolution</Label>
                  <select
                    value={resolution.label}
                    onChange={(e) => {
                      const selected = RESOLUTIONS.find(r => r.label === e.target.value);
                      if (selected) setResolution(selected);
                    }}
                    className="w-full p-2 border rounded"
                  >
                    {RESOLUTIONS.map(res => (
                      <option key={res.label} value={res.label}>
                        {res.label} ({res.width}×{res.height})
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <Label>Orientation</Label>
                  <div className="flex gap-2">
                    <Button
                      variant={orientation === 'LANDSCAPE' ? 'default' : 'outline'}
                      onClick={() => setOrientation('LANDSCAPE')}
                      className="flex-1"
                    >
                      Landscape
                    </Button>
                    <Button
                      variant={orientation === 'PORTRAIT' ? 'default' : 'outline'}
                      onClick={() => setOrientation('PORTRAIT')}
                      className="flex-1"
                    >
                      Portrait
                    </Button>
                  </div>
                </div>
              </div>

              {/* Drawing Tools */}
              <div className="space-y-4">
                <h3 className="font-semibold text-lg">Drawing Tools</h3>
                
                <Button
                  variant={isDrawingMode ? "default" : "outline"}
                  onClick={() => setIsDrawingMode(!isDrawingMode)}
                  className="w-full"
                >
                  <Plus className="h-4 w-4 mr-2" />
                  {isDrawingMode ? "Drawing Mode (Click & Drag)" : "Draw Custom Area"}
                </Button>
                
                {isDrawingMode && (
                  <div className="text-sm text-blue-600 bg-blue-50 p-3 rounded">
                    Click and drag on the canvas to create a new section
                  </div>
                )}
                
                {sections.length > 0 && (
                  <div className="text-xs text-gray-600 bg-gray-50 p-2 rounded">
                    <strong>Overlay Tip:</strong> Sections created later will appear on top of earlier sections during playback. Layer {sections.length + 1} will be created next.
                  </div>
                )}
              </div>

              {/* Sections List */}
              <div className="space-y-4">
                <h3 className="font-semibold text-lg">Sections ({sections.length})</h3>
                
                {sections.length === 0 ? (
                  <div className="text-sm text-gray-500 text-center py-8 px-4">
                    <Layers className="h-8 w-8 mx-auto mb-2 text-gray-400" />
                    <p className="font-medium mb-1">No sections created yet</p>
                    <p>Click "Draw Custom Area" and then drag on the canvas to create your first section.</p>
                  </div>
                ) : (
                  <div className="space-y-2">
                    {sections.map((section) => (
                      <div
                        key={section.id}
                        className={`p-3 border rounded cursor-pointer transition-colors ${
                          selectedSectionId === section.id
                            ? 'border-blue-500 bg-blue-50'
                            : 'border-gray-200 hover:bg-gray-50'
                        }`}
                        onClick={() => setSelectedSectionId(section.id)}
                      >
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-2">
                            {section.type === 'media' ? (
                              <ImageIcon className="h-4 w-4 text-blue-500" />
                            ) : (
                              <Type className="h-4 w-4 text-purple-500" />
                            )}
                            <span className="font-medium">{section.name}</span>
                          </div>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={(e) => {
                              e.stopPropagation();
                              removeSection(section.id);
                            }}
                            className="h-6 w-6 p-0 text-red-500 hover:text-red-700"
                          >
                            <X className="h-3 w-3" />
                          </Button>
                        </div>
                        <div className="text-xs text-gray-500 mt-1">
                          {Math.round(section.width)}% × {Math.round(section.height)}% (Layer {section.order + 1})
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Selected Section Properties */}
              {selectedSection && (
                <div className="space-y-4">
                  <h3 className="font-semibold text-lg">Section Properties</h3>
                  
                  <div>
                    <Label>Section Name</Label>
                    <Input
                      value={selectedSection.name}
                      onChange={(e) => updateSection(selectedSection.id, { name: e.target.value })}
                    />
                  </div>
                  
                  <div>
                    <Label>Content Type</Label>
                    <select
                      value={selectedSection.type}
                      onChange={(e) => updateSection(selectedSection.id, { type: e.target.value as 'media' | 'text' })}
                      className="w-full p-2 border rounded"
                    >
                      <option value="media">Media (Images/Videos)</option>
                      <option value="text">Scrolling Text</option>
                    </select>
                  </div>
                  
                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <Label>X (%)</Label>
                      <Input
                        type="number"
                        value={Math.round(selectedSection.x)}
                        onChange={(e) => updateSection(selectedSection.id, { x: parseFloat(e.target.value) || 0 })}
                        min="0"
                        max="100"
                      />
                    </div>
                    <div>
                      <Label>Y (%)</Label>
                      <Input
                        type="number"
                        value={Math.round(selectedSection.y)}
                        onChange={(e) => updateSection(selectedSection.id, { y: parseFloat(e.target.value) || 0 })}
                        min="0"
                        max="100"
                      />
                    </div>
                    <div>
                      <Label>Width (%)</Label>
                      <Input
                        type="number"
                        value={Math.round(selectedSection.width)}
                        onChange={(e) => updateSection(selectedSection.id, { width: parseFloat(e.target.value) || 1 })}
                        min="1"
                        max="100"
                      />
                    </div>
                    <div>
                      <Label>Height (%)</Label>
                      <Input
                        type="number"
                        value={Math.round(selectedSection.height)}
                        onChange={(e) => updateSection(selectedSection.id, { height: parseFloat(e.target.value) || 1 })}
                        min="1"
                        max="100"
                      />
                    </div>
                  </div>
                </div>
              )}
            </div>

            {/* Right Panel - Canvas */}
            <div className="flex-1 flex flex-col overflow-hidden">
              <div className="text-sm text-gray-600 mb-4 text-center">
                <div className="flex items-center justify-center gap-4 mb-2">
                  <span>Canvas: {resolution.width} × {resolution.height}</span>
                  <span className="text-xs">
                    {Math.round(canvasScale * 100)}% zoom
                  </span>
                </div>
                <div className="text-xs text-gray-500">
                  {isDrawingMode ? 'Click and drag anywhere on the canvas to create a section' : 'Select drawing mode to add sections'}
                </div>
                <div className="text-xs text-blue-600">
                  Use side buttons or Ctrl+/Ctrl- to zoom • Ctrl+0 to fit to view
                </div>
                <div className="text-xs text-gray-400">
                  Coordinates: Top-Left (0,0) → Bottom-Right (100,100)
                </div>
              </div>
              
              <div className="flex-1 flex items-center justify-center relative overflow-hidden">
                {/* Zoom Out Button - Left Side */}
                <div className="absolute left-4 top-1/2 transform -translate-y-1/2 z-10">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setCanvasScale(Math.max(canvasScale - 0.1, 0.2))}
                    className="h-10 w-10 rounded-full bg-white shadow-lg hover:shadow-xl border-2"
                    title="Zoom Out"
                  >
                    <span className="text-lg font-bold">-</span>
                  </Button>
                </div>

                {/* Zoom In Button - Right Side */}
                <div className="absolute right-4 top-1/2 transform -translate-y-1/2 z-10">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setCanvasScale(Math.min(canvasScale + 0.1, 3.0))}
                    className="h-10 w-10 rounded-full bg-white shadow-lg hover:shadow-xl border-2"
                    title="Zoom In"
                  >
                    <span className="text-lg font-bold">+</span>
                  </Button>
                </div>

                {/* Canvas Container */}
                <div className="flex-1 flex items-center justify-center p-4 overflow-auto bg-gray-50">
                  <div className="flex flex-col items-center">
                    <div 
                      ref={canvasRef}
                      className={`relative border-2 border-gray-300 bg-white shadow-lg ${
                        isDrawingMode ? 'cursor-crosshair' : 'cursor-default'
                      }`}
                      style={{
                        width: canvasWidth,
                        height: canvasHeight,
                        minWidth: canvasWidth,
                        minHeight: canvasHeight,
                        margin: '20px', // Add margin for better visibility
                        backgroundImage: `
                          linear-gradient(rgba(0,0,0,0.1) 1px, transparent 1px),
                          linear-gradient(90deg, rgba(0,0,0,0.1) 1px, transparent 1px)
                        `,
                        backgroundSize: `${canvasWidth / 20}px ${canvasHeight / 20}px`,
                      }}
                      onMouseDown={handleCanvasMouseDown}
                    >
                {/* Corner markers for coordinate reference */}
                <div className="absolute top-1 left-1 text-xs text-gray-400 bg-white bg-opacity-75 px-1 rounded">
                  (0,0)
                </div>
                <div className="absolute top-1 right-1 text-xs text-gray-400 bg-white bg-opacity-75 px-1 rounded">
                  (100,0)
                </div>
                <div className="absolute bottom-1 left-1 text-xs text-gray-400 bg-white bg-opacity-75 px-1 rounded">
                  (0,100)
                </div>
                <div className="absolute bottom-1 right-1 text-xs text-gray-400 bg-white bg-opacity-75 px-1 rounded">
                  (100,100)
                </div>
                {/* Empty state when no sections */}
                {sections.length === 0 && !isDrawingMode && (
                  <div className="absolute inset-0 flex items-center justify-center">
                    <div className="text-center text-gray-400">
                      <Layers className="h-12 w-12 mx-auto mb-3" />
                      <p className="text-lg font-medium mb-1">Empty Canvas</p>
                      <p className="text-sm">Enable drawing mode to start creating sections</p>
                    </div>
                  </div>
                )}
                
                {/* Drawing mode instruction */}
                {sections.length === 0 && isDrawingMode && (
                  <div className="absolute inset-0 flex items-center justify-center">
                    <div className="text-center text-blue-600">
                      <div className="text-lg font-medium mb-1">Drawing Mode Active</div>
                      <p className="text-sm">Click and drag to create your first section</p>
                    </div>
                  </div>
                )}
                {/* Existing sections */}
                {sections.map((section) => (
                  <div
                    key={section.id}
                    className={`absolute border-2 rounded transition-all ${
                      selectedSectionId === section.id
                        ? 'border-blue-500 bg-blue-100 bg-opacity-50'
                        : section.type === 'text'
                        ? 'border-purple-300 bg-purple-50 bg-opacity-50'
                        : 'border-gray-400 bg-gray-100 bg-opacity-50'
                    } hover:border-blue-400 cursor-pointer`}
                    style={{
                      left: `${section.x}%`,
                      top: `${section.y}%`,
                      width: `${section.width}%`,
                      height: `${section.height}%`,
                      zIndex: section.order + 1, // Higher order = higher z-index for proper overlaying
                    }}
                    onClick={(e) => handleSectionClick(e, section)}
                  >
                    <div className="absolute inset-0 flex items-center justify-center">
                      <div className="text-center">
                        <div className="flex items-center justify-center gap-1 mb-1">
                          {section.type === 'media' ? (
                            <ImageIcon className="h-3 w-3" />
                          ) : (
                            <Type className="h-3 w-3" />
                          )}
                          <span className="text-xs bg-white bg-opacity-75 px-1 rounded">
                            L{section.order + 1}
                          </span>
                        </div>
                        <div className="text-xs font-medium truncate px-1">
                          {section.name}
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
                
                {/* Drawing preview */}
                {drawingRect && (
                  <div
                    className="absolute border-2 border-blue-500 bg-blue-200 bg-opacity-30"
                    style={{
                      left: `${drawingRect.x}%`,
                      top: `${drawingRect.y}%`,
                      width: `${drawingRect.width}%`,
                      height: `${drawingRect.height}%`,
                    }}
                  />
                )}
                    </div>
                    
                    {/* Reset Zoom Button */}
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => {
                        // Calculate fit-to-container scale
                        const container = canvasRef.current?.parentElement;
                        if (container) {
                          const containerWidth = container.clientWidth;
                          const containerHeight = container.clientHeight;
                          const padding = 60;
                          
                          const maxWidth = containerWidth - padding;
                          const maxHeight = containerHeight - padding;
                          
                          const scaleX = maxWidth / resolution.width;
                          const scaleY = maxHeight / resolution.height;
                          
                          const fitScale = Math.min(scaleX, scaleY, 1.0);
                          setCanvasScale(Math.max(fitScale, 0.3));
                        }
                      }}
                      className="mt-2 text-xs"
                      title="Fit to container"
                    >
                      Fit to View
                    </Button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button onClick={handleSave} disabled={sections.length === 0 || !layoutName.trim()}>
              <Save className="h-4 w-4 mr-2" />
              Save Layout
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Section Naming Dialog */}
      <Dialog open={namingDialogOpen} onOpenChange={setNamingDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Name Your Section</DialogTitle>
          </DialogHeader>
          
          <div className="space-y-4 py-4">
            <div>
              <Label>Section Name</Label>
              <Input
                value={sectionName}
                onChange={(e) => setSectionName(e.target.value)}
                placeholder="Enter section name"
                autoFocus
              />
            </div>
            
            <div>
              <Label>Content Type</Label>
              <div className="flex gap-2">
                <Button
                  variant={sectionType === 'media' ? 'default' : 'outline'}
                  onClick={() => setSectionType('media')}
                  className="flex-1"
                >
                  <ImageIcon className="h-4 w-4 mr-2" />
                  Media
                </Button>
                <Button
                  variant={sectionType === 'text' ? 'default' : 'outline'}
                  onClick={() => setSectionType('text')}
                  className="flex-1"
                >
                  <Type className="h-4 w-4 mr-2" />
                  Text
                </Button>
              </div>
            </div>
          </div>
          
          <DialogFooter>
            <Button variant="outline" onClick={cancelSectionCreation}>
              Cancel
            </Button>
            <Button onClick={confirmSectionCreation} disabled={!sectionName.trim()}>
              Create Section
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}