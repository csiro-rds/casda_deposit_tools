import csv
import astropy
from astropy.io.votable.tree import VOTableFile, Resource, Table, Field, CooSys, Param
from astropy import units as u
from astropy.coordinates import SkyCoord

#Note: Once run the following changes will be needed to the xml before ingesting:
# 1. Change the votable header to v1.3.
# 2. Add the following node unde rthe VOTable:
#   <COOSYS ID="J2000" equinox="J2000" system="eq_FK5"/>
# 3. Change unit="arcs" to unit="arcsec"
# 4. Move the params above the fields

SourceFilename='C:\\temp\\CDFS_published_Table4.txt'
CSVFilename='C:\\temp\\CDFS_published_Table4.csv'
XMLFilename='C:\\temp\\CDFS_published_Table4.xml'

Lines=0
Section=0

StartPos=[]
FinishPos=[]
ColumnNameList=[]
DescriptionList=[]
UnitList=[]
List=[]

Output=csv.writer(open(CSVFilename,'w'),quoting=csv.QUOTE_ALL,lineterminator='\n')

for Text in open(SourceFilename,'r'):
	Text=Text.rstrip()
	
	if Text[0:5]=='=====':
		Section=Section+1
		Text=""
	if Text[0:5]=='-----':
		Section=Section+1
		if Section==5:
			# Write the header row.
			Output.writerow(ColumnNameList)
			Output.writerow(UnitList)
			Output.writerow(DescriptionList)
		Text=""
	
	if len(Text)>0:
		#print (Text)
		Lines=Lines+1
		if Section==3:
			# Column data
			# Section 3 defines the table structure.
			print (Text[1:4],end="")	# Start byte
			print (Text[5:8],end="")	# Finish byte
			print (Text[9:10],end="")	# Type
			print (Text[16:23],end="")	# Units
			print (Text[25:32],end="")	# Column name
			print (Text[34:])			# Comment
			StartPos.append(Text[1:4])
			FinishPos.append(Text[5:8])
			if StartPos[len(StartPos)-1]=='   ':
				StartPos[len(StartPos)-1]=Text[5:8]
			
			Type=Text[9:10]
			
			Units=Text[16:23]
			Units=Units.rstrip()
			if Units=='---':
				Units=''
			elif Units=='uJy':
				Units='microJy'
			UnitList.append(Units)
			
			Fieldname=Text[25:32]
			Fieldname=Fieldname.rstrip()
			if Fieldname=='ID':
				Fieldname='component_id'
			elif Fieldname=='Name':
				Fieldname='component_name'
			elif Fieldname=='PFlux':
				Fieldname='flux_peak'
			elif Fieldname=='IFlux':
				Fieldname='flux_int'
			elif Fieldname=='MajAxis':
				Fieldname='maj_axis_deconv'
			elif Fieldname=='MinAxis':
				Fieldname='min_axis_deconv'
			elif Fieldname=='PosAng':
				Fieldname='pos_ang_deconv'
			elif Fieldname=='rms':
				Fieldname='rms_image'
			elif Fieldname=='Com':
				Fieldname='Comment'
			ColumnNameList.append(Fieldname)	
			Description=Text[34:]
			DescriptionList.append(Description)

		if Section==5:
			# Row data
			Row=[]
			for c in range(0,len(StartPos)):
				#print (ColumnName[c],Text[int(StartPos[c])-1:int(FinishPos[c])])
				Result=Text[int(StartPos[c])-1:int(FinishPos[c])]
				Row.append(Result)			
			# Write row to CSV file
			Output.writerow(Row)			
			List.append(Row)
			
votable=VOTableFile()
#coosys=CooSys(ID="J2000", equinox="J2000", system="eq_FK5")
#votable.coordinate_systems.append(coosys)
resource=Resource()
votable.resources.append(resource)
table=Table(votable)
resource.tables.append(table)
table.params.extend([
	Param(votable, name="imageFile", ucd="meta.file;meta.fits", datatype="char", arraysize="255", value="atlas-cdfs.fits"),
	Param(votable, name="Reference frequency", ucd="em.freq;meta.main", datatype="float", unit="Hz", value="1.408e+08")])
table.fields.extend([
	Field(votable, name="island_id",datatype="char",unit="--",arraysize="15"),
	Field(votable, name="component_id",datatype="char",unit="--",arraysize="4"),
	Field(votable, name="component_name",datatype="char",unit="",arraysize="26"),
	Field(votable, name="ra_hms_cont", datatype="char",unit="",arraysize="12",ref="J2000"),
	Field(votable, name="dec_dms_cont", datatype="char", unit="",arraysize="13",ref="J2000"),
	Field(votable, name="ra_deg_cont", datatype="float",precision="6", unit="deg",ucd="pos.eq.ra;meta.main",ref="J2000",width="12"),
	Field(votable, name="dec_deg_cont", datatype="float",precision="6", unit="deg",ucd="pos.eq.dec;meta.main",ref="J2000",width="13"),
	Field(votable, name="ra_err", datatype="float",precision="2",unit="arcsec",ref="J2000",width="11"),
	Field(votable, name="dec_err", datatype="float",precision="2",unit="arcsec",ref="J2000",width="11"),
	Field(votable, name="freq", datatype="float",precision="1",unit="MHz", ucd="em.freq",width="11"),
	Field(votable, name="flux_peak", datatype="float",precision="3", unit="mJy/beam",width="11"),
	Field(votable, name="flux_peak_err", datatype="float",precision="3", unit="mJy/beam",width="14"),
	Field(votable, name="flux_int",datatype="float",precision="3",unit="mJy",width="10"),
	Field(votable, name="flux_int_err", datatype="float",precision="3", unit="mJy",width="13"),
	Field(votable, name="maj_axis", datatype="float",precision="2", unit="arcsec",width="9"),
	Field(votable, name="min_axis", datatype="float",precision="2", unit="arcsec",width="9"),
	Field(votable, name="pos_ang", datatype="float",precision="2", unit="deg",width="8"),
	Field(votable, name="maj_axis_err", datatype="float",precision="2", unit="arcsec",width="13"),
	Field(votable, name="min_axis_err", datatype="float",precision="2", unit="arcsec",width="13"),
	Field(votable, name="pos_ang_err", datatype="float",precision="2", unit="deg",width="12"),
	Field(votable, name="maj_axis_deconv",datatype="float",precision="2",unit="arcsec",width="18"),
	Field(votable, name="min_axis_deconv",datatype="float",precision="2",unit="arcsec",width="16"),
	Field(votable, name="pos_ang_deconv",datatype="float",precision="2",unit="deg",width="15"),
	Field(votable, name="chi_squared_fit",datatype="float",precision="4",unit="--",width="17"),
	Field(votable, name="rms_fit_gauss",datatype="float",precision="3",unit="mJy/beam",width="15"),
	Field(votable, name="spectral_index",datatype="float",precision="2",unit="--",width="15"),
	Field(votable, name="spectral_curvature",datatype="float",precision="2",unit="--",width="19"),
	Field(votable, name="rms_image", datatype="float", precision="3" ,unit="mJy/beam",width="12"),
	Field(votable, name="flag_c1", datatype="int", unit="", ucd="meta.code",width="8"),
	Field(votable, name="flag_c2", datatype="int", unit="", ucd="meta.code",width="8"),
	Field(votable, name="flag_c3", datatype="int", unit="", ucd="meta.code",width="8"),
	Field(votable, name="flag_c4", datatype="int", unit="", ucd="meta.code",width="8"),
	Field(votable, name="comment",datatype="char",unit="",arraysize="115")]) # Need to allow for & being expanded

	
print (len(List))
table.create_arrays(len(List))
for r in range(0,len(List)):
	#print (List[r][0])
	x=List[r]
	component_id=x[0]
	component_name=x[1]
	RAh=int(x[2])
	RAm=int(x[3])
	RAs=float(x[4])
	DE_sign=x[5]
	DEd=int(x[6])
	DEm=int(x[7])
	DEs=float(x[8])
	ra_err=float(x[9]) 
	dec_err=float(x[10])
	flux_peak=float(x[11])
	flux_int=float(x[12]) 
	maj_axis_deconv=float(x[13])
	min_axis_deconv=float(x[14])
	pos_ang_deconv=float(x[15])
	rms_image_ujy=float(x[16]) 
	rms_image=rms_image_ujy/1000 # Convert to mJy/beam (from uJy/beam)
	comment=x[17]
	ra_hms_cont=x[2]+":"+x[3]+":"+x[4]
	dec_dms_cont=x[5]+x[6]+":"+x[7]+":"+x[8]
	c=SkyCoord(x[2]+'h'+x[3]+'m'+x[4]+'s',x[5]+x[6]+'d'+x[7]+'m'+x[8]+'s','icrs')
	island_id=""
	ra_deg_cont=c.ra.deg
	dec_deg_cont=c.dec.degree
	freq=1408
	flux_peak_err=0
	flux_int_err=0
	maj_axis=maj_axis_deconv
	min_axis=min_axis_deconv
	pos_ang=pos_ang_deconv
	maj_axis_err=0
	min_axis_err=0
	pos_ang_err=0
	chi_squared_fit=-1
	rms_fit_gauss=-1
	spectral_index=0
	spectral_curvature=0
	flag_c1=0
	flag_c2=0
	flag_c3=0
	flag_c4=0
	
	z=island_id,component_id,component_name,ra_hms_cont,dec_dms_cont,ra_deg_cont,dec_deg_cont,ra_err,dec_err,freq,flux_peak, \
		flux_peak_err,flux_int,flux_int_err,maj_axis,min_axis,pos_ang,maj_axis_err,min_axis_err,pos_ang_err, \
		maj_axis_deconv,min_axis_deconv,pos_ang_deconv,chi_squared_fit,rms_fit_gauss,spectral_index,spectral_curvature, \
		rms_image,flag_c1,flag_c2,flag_c3,flag_c4,comment
	table.array[r]=z

votable.to_xml(XMLFilename)



