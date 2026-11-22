import bpy, math, os
from mathutils import Vector
OUT='/Volumes/ORBIT/Nivoryn/cinematic/product-led-v1'
os.makedirs(OUT+'/renders',exist_ok=True)
scene=bpy.data.scenes.new('Nivoryn_Product_Evidence_v1')
bpy.context.window.scene=scene
scene.render.engine='CYCLES'; scene.cycles.samples=32
scene.cycles.use_denoising=True
scene.render.film_transparent=True
scene.render.image_settings.file_format='PNG';scene.render.image_settings.color_mode='RGBA'
scene.render.resolution_percentage=100
scene.view_settings.view_transform='AgX'
world=bpy.data.worlds.new('Product studio world');scene.world=world;world.use_nodes=True
world.node_tree.nodes['Background'].inputs['Color'].default_value=(.35,.35,.35,1)
world.node_tree.nodes['Background'].inputs['Strength'].default_value=.45

def mat(name,color,metal=0,rough=.4):
 m=bpy.data.materials.new(name);m.diffuse_color=(*color,1);m.use_nodes=True
 p=m.node_tree.nodes.get('Principled BSDF');p.inputs['Base Color'].default_value=(*color,1);p.inputs['Metallic'].default_value=metal;p.inputs['Roughness'].default_value=rough
 return m
graphite=mat('Graphite satin',(.033,.036,.04),.55,.28)
edge=mat('Machined graphite rim',(.13,.14,.15),.85,.23)
black=mat('Black glass',(.009,.012,.015),.1,.18)
paper=mat('Warm ivory paper',(.89,.89,.86),0,.73)
red=mat('Signal red detail',(.88,.055,.065),.15,.38)
fold=mat('Envelope folds',(.049,.052,.058),.42,.39)

def cube(name,loc,scale,material,bevel=.08):
 bpy.ops.mesh.primitive_cube_add(size=1,location=loc);o=bpy.context.object;o.name=name;o.dimensions=scale
 bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
 o.data.materials.append(material)
 if bevel:
  mod=o.modifiers.new('Editable precision edge','BEVEL');mod.width=bevel;mod.segments=5
  o.modifiers.new('Weighted corner normals','WEIGHTED_NORMAL')
 return o

def poly(name,verts,material):
 me=bpy.data.meshes.new(name);me.from_pydata(verts,[],[list(range(len(verts)))]);me.materials.append(material)
 o=bpy.data.objects.new(name,me);scene.collection.objects.link(o)
 sol=o.modifiers.new('Paper thickness','SOLIDIFY');sol.thickness=.018
 bevel=o.modifiers.new('Soft cut edges','BEVEL');bevel.width=.015;bevel.segments=3
 return o

# Each object collection is rendered through the same orthographic studio rig.
collections={}
def group(name,fn):
 before=set(scene.objects);fn();objs=set(scene.objects)-before
 col=bpy.data.collections.new(name);scene.collection.children.link(col)
 for o in objs:
  for old in list(o.users_collection):old.objects.unlink(o)
  col.objects.link(o)
 collections[name]=col

def phone():
 cube('Phone aluminum unibody',(0,0,0),(3.35,6.65,.29),edge,.25)
 cube('Phone graphite bezel',(0,0,.165),(3.25,6.55,.13),black,.23)
 cube('Ivory screen; live DOM aligns above this',(0,-.02,.245),(2.96,6.03,.035),paper,.16)
 cube('Earpiece',(0,3.12,.265),(.44,.045,.02),graphite,.02)
 cube('Volume rocker',(-1.704,1.48,0),(.055,.65,.1),edge,.02)
 cube('Power button',(1.704,1.16,0),(.055,.56,.1),red,.02)
 cube('Lower screen accent',(0,-3.12,.265),(.6,.035,.015),graphite,.012)
group('PHONE · editable unibody',phone)
def envelope():
 cube('Envelope body',(0,-.22,-.02),(4.6,2.9,.15),graphite,.07)
 cube('Ivory message insert',(0,.59,.04),(4.14,2.68,.04),paper,.04)
 poly('Left fold',[(-2.3,1.22,.09),(0,-.29,.21),(-2.3,-1.67,.09)],fold)
 poly('Right fold',[(2.3,1.22,.09),(2.3,-1.67,.09),(0,-.29,.21)],graphite)
 poly('Lower fold',[(-2.3,-1.67,.1),(2.3,-1.67,.1),(0,.12,.24)],fold)
 cube('Red fold seal',(.03,-.66,.245),(.4,.4,.028),red,.025)
group('ENVELOPE · message carrier',envelope)
def urlstrip():
 cube('URL graphite specimen',(0,0,0),(5.5,1,.18),edge,.13)
 cube('URL ivory inset',(0,0,.11),(5.26,.77,.03),paper,.055)
 cube('Actual host marker',(-2.31,0,.15),(.075,.37,.025),red,.015)
group('URL · destination strip',urlstrip)
def document():
 cube('Document graphite backing',(0,0,-.03),(3.45,4.65,.15),graphite,.13)
 poly('Document cut corner',[(-1.55,-2.12,.1),(1.55,-2.12,.1),(1.55,1.58,.1),(1.04,2.13,.1),(-1.55,2.13,.1)],paper)
 poly('Document folded corner',[(1.04,2.13,.11),(1.04,1.58,.14),(1.55,1.58,.11)],edge)
 cube('Document signal accent',(-1.14,1.57,.14),(.1,.43,.025),red,.01)
group('DOCUMENT · unverified specimen',document)

bpy.ops.object.camera_add(location=(0,0,14));cam=bpy.context.object;cam.name='Product orthographic camera';cam.rotation_euler=(0,0,0);cam.rotation_euler=(Vector((0,0,0))-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.type='ORTHO';scene.camera=cam
for name,loc,power,size in [('Large softbox',(-4,5,7),700,6),('Edge strip',(4,1,5),950,4),('Low fill',(-1,-5,3),190,3)]:
 bpy.ops.object.light_add(type='AREA',location=loc);l=bpy.context.object;l.name=name;l.data.energy=power;l.data.shape='DISK';l.data.size=size;l.rotation_euler=(Vector((0,0,0))-l.location).to_track_quat('-Z','Y').to_euler()
# Preserve all previous scenes in this separately named file, with new scene active.
for name,col in collections.items():col.hide_render=name!='PHONE · editable unibody'
cam.data.ortho_scale=7.5
scene.render.resolution_x=640;scene.render.resolution_y=1200
bpy.ops.wm.save_as_mainfile(filepath=OUT+'/Nivoryn-Product-Evidence.blend')
for name,key,w,h,scale in [('PHONE · editable unibody','phone',640,1200,7.5),('ENVELOPE · message carrier','envelope',1000,760,4.5),('URL · destination strip','url',1100,260,1.3),('DOCUMENT · unverified specimen','document',640,840,5.1)]:
 for n,col in collections.items():col.hide_render=n!=name
 scene.render.resolution_x=w;scene.render.resolution_y=h;cam.data.ortho_scale=scale
 scene.render.filepath=OUT+'/renders/'+key+'.png';bpy.ops.render.render(write_still=True)
# Set a composed editable studio overview in saved file.
for n,col in collections.items():col.hide_render=False
for n,dx,dy,angle in [('PHONE · editable unibody',.6,.1,-8),('ENVELOPE · message carrier',-3,-.5,12),('URL · destination strip',-2,-3,-8),('DOCUMENT · unverified specimen',3,0,-10)]:
 empty=bpy.data.objects.new(n+' assembly control',None);scene.collection.objects.link(empty)
 for o in collections[n].objects:o.parent=empty
 empty.location=(dx,dy,0);empty.rotation_euler.z=math.radians(angle)
cam.data.ortho_scale=12;scene.render.resolution_x=1600;scene.render.resolution_y=1300
bpy.ops.wm.save_as_mainfile(filepath=OUT+'/Nivoryn-Product-Evidence.blend')
print('PRODUCT_ASSETS_READY',OUT)
